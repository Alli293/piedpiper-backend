package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsula la llamada a Gemini para buscar alternativas ambientales equivalentes a una actividad
 * del itinerario. Sigue el mismo patrón de reintentos y manejo de errores que
 * {@link ItinerarioIaClienteService}, con MAX_REINTENTOS = 2.
 *
 * <p>Timeout o falla de red falla inmediatamente sin reintento. Una respuesta que llega pero es
 * null o vacía se reintenta hasta {@value #MAX_REINTENTOS} veces.
 */
@Service
public class AlternativasIaClienteService {

    private static final Logger log = LoggerFactory.getLogger(AlternativasIaClienteService.class);

    static final int MAX_REINTENTOS = 2;

    static final String SYSTEM_MESSAGE = "Eres un asesor de turismo sostenible en Costa Rica. Tu tarea es sugerir alternativas "
            + "con mejor desempeño ambiental para una actividad turística específica. "
            + "Respondes ÚNICAMENTE con JSON válido sin texto adicional.";

    private final ChatClient chatClient;
    private final String geminiApiKey;

    public AlternativasIaClienteService(ChatClient.Builder chatClientBuilder,
                                         @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Busca alternativas ambientales equivalentes para una actividad dada, consultando a Gemini.
     * Reintenta hasta {@value #MAX_REINTENTOS} veces si la respuesta es estructuralmente inválida.
     *
     * @param actividadOriginal la actividad para la cual se buscan alternativas
     * @param nombresExcluidos  nombres de actividades a excluir de los resultados
     * @return lista de alternativas sugeridas por Gemini
     * @throws ApiException {@code itinerarioGeneracionTimeout()} si la llamada falla (timeout, red);
     *                      {@code itinerarioRespuestaInvalida()} si se agotan los reintentos.
     */
    public List<AlternativaIaDTO> buscarAlternativas(ItinerarioActividad actividadOriginal,
                                                      List<String> nombresExcluidos) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.error("GEMINI_API_KEY no está configurada");
            throw ApiException.itinerarioGeneracionTimeout();
        }

        String promptUsuario = construirPromptUsuario(actividadOriginal, nombresExcluidos);

        for (int intento = 1; intento <= MAX_REINTENTOS; intento++) {
            RespuestaAlternativasIA respuesta = invocarChatClient(promptUsuario);

            if (respuesta != null && respuesta.alternativas() != null && !respuesta.alternativas().isEmpty()) {
                return respuesta.alternativas();
            }

            log.warn("Respuesta de IA inválida al buscar alternativas (intento {}/{})",
                    intento, MAX_REINTENTOS);
        }

        throw ApiException.itinerarioRespuestaInvalida();
    }

    private String construirPromptUsuario(ItinerarioActividad actividad, List<String> nombresExcluidos) {
        String nombre = actividad.getNombre();
        String categoriaTuristica = actividad.getCategoriaTuristica() != null
                ? actividad.getCategoriaTuristica().name()
                : "GENERAL";
        String provincia = actividad.getProvincia() != null
                ? actividad.getProvincia().name()
                : "SAN_JOSE";
        String excluidos = nombresExcluidos.stream().collect(Collectors.joining(", "));

        return "Sugiere entre 3 y 5 alternativas con mejor desempeño ambiental para la siguiente actividad:\n"
                + "- Nombre: " + nombre + "\n"
                + "- Categoría: " + categoriaTuristica + "\n"
                + "- Provincia: " + provincia + "\n\n"
                + "Requisitos:\n"
                + "- Todas las alternativas deben ser de la misma categoría (" + categoriaTuristica + ") y provincia (" + provincia + ").\n"
                + "- No incluir estas actividades: " + excluidos + "\n"
                + "- Para cada alternativa incluir: nombre, descripcion, costoAproximado (número en CRC), moneda (\"CRC\"), establecimientoRecomendado, puntuacionAmbientalEstimada (entero 0-100).\n"
                + "- Priorizar opciones con certificaciones ambientales, menor huella de carbono, uso responsable de recursos naturales.\n"
                + "- Responde con JSON: {\"alternativas\": [{...}]}";
    }

    private RespuestaAlternativasIA invocarChatClient(String promptUsuario) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(promptUsuario)
                    .call()
                    .entity(RespuestaAlternativasIA.class);
        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini para buscar alternativas: {}", e.getMessage(), e);
            throw ApiException.itinerarioGeneracionTimeout();
        }
    }

    /**
     * Record interno para deserializar la respuesta JSON de Gemini que contiene la lista de alternativas.
     */
    record RespuestaAlternativasIA(List<AlternativaIaDTO> alternativas) {}
}
