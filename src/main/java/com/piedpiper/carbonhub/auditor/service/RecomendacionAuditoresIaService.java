package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.common.IaRateLimitService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Encapsula ÚNICAMENTE la llamada a Gemini que redacta las justificaciones de la recomendación de
 * auditores (PP-57). El orden y la selección de los candidatos ya los decidió de forma determinista
 * {@link RecomendacionAuditoresConsultaService}; la IA solo pone en palabras por qué cada uno encaja.
 *
 * <p>Se llama <b>una sola vez</b> con todos los candidatos, no una por auditor. Cualquier fallo
 * (API key ausente, cuota agotada, timeout, 429/5xx de Gemini, respuesta no parseable) se traduce a
 * {@link Optional#empty()} sin propagar la excepción: el criterio de aceptación pide que en ese caso
 * los candidatos se devuelvan igual, solo sin justificación.</p>
 *
 * <p><b>Privacidad.</b> El prompt se arma únicamente con los datos públicos que se le pasan en
 * {@link CandidatoIa}. El nombre de la empresa solicitante, su id y cualquier dato de otras empresas
 * nunca entran aquí: este servicio no recibe la entidad {@code Empresa}, solo el nombre del sector.</p>
 */
@Service
public class RecomendacionAuditoresIaService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionAuditoresIaService.class);

    static final String SYSTEM_MESSAGE = "Eres un asistente de selección de auditores ambientales. "
            + "Responde en español de Costa Rica, en tono claro y profesional. Para cada auditor "
            + "listado, genera una justificación de máximo 2 oraciones que explique por qué es "
            + "adecuado para la solicitud, basándote únicamente en los datos provistos. No inventes "
            + "información. No menciones nombres de empresas.";

    private final ChatClient chatClient;
    private final IaRateLimitService iaRateLimitService;
    private final String geminiApiKey;

    public RecomendacionAuditoresIaService(
            ChatClient.Builder chatClientBuilder,
            IaRateLimitService iaRateLimitService,
            @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.iaRateLimitService = iaRateLimitService;
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Devuelve la justificación por {@code auditorId}. Vacío si la IA no pudo generar el lote: en ese
     * caso el llamador incluye a los candidatos sin justificación.
     */
    public Optional<Map<UUID, String>> generarJustificaciones(
            UUID empresaId, String sector, String tipoAuditoria, String zonaGeografica,
            List<CandidatoIa> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) {
            return Optional.empty();
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.error("GEMINI_API_KEY no está configurada");
            return Optional.empty();
        }
        if (!iaRateLimitService.reservar(empresaId)) {
            log.warn("Cuota de IA excedida para la recomendación de auditores de la empresa {}", empresaId);
            return Optional.empty();
        }

        try {
            RespuestaJustificaciones respuesta = chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(construirPromptUsuario(sector, tipoAuditoria, zonaGeografica, candidatos))
                    .call()
                    .entity(RespuestaJustificaciones.class);

            if (respuesta == null || respuesta.justificaciones() == null
                    || respuesta.justificaciones().size() != candidatos.size()) {
                log.warn("Respuesta de IA inválida o con un número de justificaciones distinto al de candidatos");
                return Optional.empty();
            }

            return Optional.of(emparejar(candidatos, respuesta.justificaciones()));
        } catch (Exception e) {
            log.warn("Error generando las justificaciones de la recomendación vía IA: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Empareja cada justificación con su auditor, y descarta la que no se pueda atribuir con
     * certeza.
     *
     * <p>Emparejar por posición a secas seria confiar en que el modelo respeta el orden en que se
     * le mandaron los candidatos. Si algún día no lo respeta, cada auditor recibiria la
     * justificación de otro: texto plausible, atribuido a quien no corresponde, y sin ninguna señal
     * de que algo salió mal. En una pantalla que existe para ayudar a elegir un auditor, eso es
     * bastante peor que no mostrar justificación.</p>
     *
     * <p>Por eso se comprueba contra el nombre que el propio modelo devuelve. La comparación es
     * laxa (sin tildes, sin mayúsculas y por prefijo) porque el modelo suele acortar "Ana Mora
     * Vargas" a "Ana Mora", y descartar por eso seria tirar justificaciones buenas.</p>
     */
    Map<UUID, String> emparejar(List<CandidatoIa> candidatos, List<JustificacionIa> justificaciones) {
        Map<UUID, String> porAuditor = new HashMap<>();
        for (int i = 0; i < candidatos.size(); i++) {
            CandidatoIa candidato = candidatos.get(i);
            JustificacionIa justificacion = justificaciones.get(i);
            String texto = justificacion.justificacion();
            if (texto == null || texto.isBlank()) {
                continue;
            }
            if (!identificaSoloA(candidato, justificacion.nombre(), candidatos)) {
                log.warn("La IA devolvió una justificación que no se puede atribuir con certeza al "
                        + "candidato de esa posición; se descarta para no asignarla a quien no es");
                continue;
            }
            porAuditor.put(candidato.auditorId(), texto.trim());
        }
        return porAuditor;
    }

    /**
     * El nombre devuelto tiene que señalar a este candidato y a ningún otro.
     *
     * <p>Comprobar solo que coincida con el candidato de esa posición no alcanza cuando el nombre
     * es ambiguo entre varios. Si compiten "Ana Mora" y "Ana Solís" y el modelo responde apenas
     * "Ana", esa respuesta encaja con las dos, así que aceptarla equivale a confiar de nuevo en el
     * orden, que es justo lo que este control existe para no hacer. Ante ambigüedad se descarta:
     * quedarse sin justificación es mucho mejor que atribuírsela a la persona equivocada.</p>
     */
    private boolean identificaSoloA(CandidatoIa candidato, String nombreRespuesta,
                                    List<CandidatoIa> candidatos) {
        if (nombreRespuesta == null || nombreRespuesta.isBlank()) {
            return true;
        }
        if (!mismoAuditor(candidato.nombre(), nombreRespuesta)) {
            return false;
        }
        return candidatos.stream()
                .filter(otro -> mismoAuditor(otro.nombre(), nombreRespuesta))
                .count() == 1;
    }

    /**
     * Un nombre ausente en la respuesta no invalida nada: solo se usa para detectar un cruce.
     *
     * <p>La comparación es por <b>palabras completas</b> y no por prefijo de cadena. Un prefijo de
     * cadena daría por bueno el cruce entre dos personas distintas cuando un nombre empieza igual
     * que otro: {@code "ana morales".startsWith("ana mora")} es cierto, y con apellidos compuestos
     * eso es habitual. Justo el caso que este control existe para detectar quedaría sin detectar.
     * Palabra por palabra, "Ana Mora" y "Ana Morales" son distintas, y "Ana Mora Vargas" acortado a
     * "Ana Mora" se sigue reconociendo, que es el motivo por el que la comparación no es exacta.</p>
     */
    private boolean mismoAuditor(String nombreCandidato, String nombreRespuesta) {
        if (nombreRespuesta == null || nombreRespuesta.isBlank()) {
            return true;
        }
        List<String> esperado = palabras(nombreCandidato);
        List<String> recibido = palabras(nombreRespuesta);
        if (esperado.isEmpty() || recibido.isEmpty()) {
            return false;
        }
        int comunes = Math.min(esperado.size(), recibido.size());
        return esperado.subList(0, comunes).equals(recibido.subList(0, comunes));
    }

    private List<String> palabras(String valor) {
        String normalizado = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalizado.isEmpty() ? List.of() : List.of(normalizado.split("\\s+"));
    }

    String construirPromptUsuario(String sector, String tipoAuditoria, String zonaGeografica,
                                  List<CandidatoIa> candidatos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Necesidad: empresa del sector ").append(sector)
                .append(" busca auditor para ").append(tipoAuditoria)
                .append(" en la zona ").append(zonaGeografica).append(". Candidatos: [");
        for (int i = 0; i < candidatos.size(); i++) {
            CandidatoIa c = candidatos.get(i);
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(c.nombre())
                    .append(", especialidades: ").append(String.join(", ", c.especialidades()))
                    .append(", calificación promedio: ").append(calificacionDe(c))
                    .append(", sectores auditados con mayor frecuencia: ")
                    .append(c.top3Sectores().isEmpty() ? "sin datos" : String.join(", ", c.top3Sectores()))
                    .append(", auditorías completadas: ").append(c.auditoriasCompletadas());
        }
        sb.append("]. Genera una justificación breve para cada candidato.");
        return sb.toString();
    }

    /**
     * El {@code /5} solo tiene sentido detrás de un número. Un auditor recién certificado todavía no
     * tiene reseñas y llega con la calificación en nulo —según el javadoc del comparador de
     * {@code RecomendacionAuditoresConsultaService}, el caso más común en un sistema nuevo—, así que
     * concatenar la escala sin mirar producía "sin calificaciones/5" en el prompt.
     */
    private String calificacionDe(CandidatoIa candidato) {
        return candidato.calificacionPromedio() == null
                ? "sin calificaciones"
                : candidato.calificacionPromedio() + "/5";
    }

    /**
     * Vista pública de un candidato para el prompt. El {@code auditorId} nunca se envía a Gemini.
     *
     * <p>{@code calificacionPromedio} viaja nulo cuando el auditor no tiene reseñas: el texto que
     * describe esa ausencia lo decide el prompt, no quien arma el candidato.</p>
     */
    public record CandidatoIa(
            UUID auditorId,
            String nombre,
            List<String> especialidades,
            String calificacionPromedio,
            List<String> top3Sectores,
            int auditoriasCompletadas) {
    }

    record RespuestaJustificaciones(List<JustificacionIa> justificaciones) {
    }

    record JustificacionIa(String nombre, String justificacion) {
    }
}
