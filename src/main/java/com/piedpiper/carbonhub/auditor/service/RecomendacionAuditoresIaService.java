package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.common.IaRateLimitService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Encapsula ÚNICAMENTE la llamada a Gemini que redacta las justificaciones de la recomendación de
 * auditores (PP-57). El orden y la selección de los candidatos ya los decidió de forma determinista
 * {@link RecomendacionAuditoresService}; la IA solo pone en palabras por qué cada uno encaja.
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

            // Se mapea por posición y no por nombre: dos auditores podrían llamarse igual, pero el
            // orden en que se enviaron es único y la IA responde en ese mismo orden.
            Map<UUID, String> porAuditor = new HashMap<>();
            for (int i = 0; i < candidatos.size(); i++) {
                String texto = respuesta.justificaciones().get(i).justificacion();
                if (texto != null && !texto.isBlank()) {
                    porAuditor.put(candidatos.get(i).auditorId(), texto.trim());
                }
            }
            return Optional.of(porAuditor);
        } catch (Exception e) {
            log.warn("Error generando las justificaciones de la recomendación vía IA: {}", e.getMessage());
            return Optional.empty();
        }
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
                    .append(", calificación promedio: ").append(c.calificacionPromedio()).append("/5")
                    .append(", sectores auditados con mayor frecuencia: ")
                    .append(c.top3Sectores().isEmpty() ? "sin datos" : String.join(", ", c.top3Sectores()))
                    .append(", auditorías completadas: ").append(c.auditoriasCompletadas());
        }
        sb.append("]. Genera una justificación breve para cada candidato.");
        return sb.toString();
    }

    /** Vista pública de un candidato para el prompt. El {@code auditorId} nunca se envía a Gemini. */
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
