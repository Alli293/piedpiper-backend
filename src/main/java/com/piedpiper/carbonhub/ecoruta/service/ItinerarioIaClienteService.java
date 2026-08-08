package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioIaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.enums.ResultadoValidacionItinerario;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Encapsula ÚNICAMENTE la llamada a Gemini para generar un itinerario: construcción del prompt ya
 * la hizo el llamador, acá solo se invoca el {@code ChatClient}, se reintenta en caso de respuesta
 * estructuralmente inválida, y se traduce cualquier falla a un {@code ApiException} controlado.
 * Vive separada de {@code EcoRutaItinerarioService} (que sabe de {@code PreferenciasViaje} y
 * persistencia) para que PP-88 (chat de refinamiento) la reutilice con un prompt distinto sin
 * duplicar el manejo de reintentos/timeout.
 *
 * <p>Distinción del criterio de aceptación: un timeout o cualquier falla de la llamada en sí
 * (>10s, red, error del proveedor) falla de inmediato, SIN reintento. Una respuesta que sí llega
 * pero no pasa {@link ItinerarioValidador} se reintenta hasta {@value #MAX_REINTENTOS_IA} veces.
 */
@Service
public class ItinerarioIaClienteService {

    private static final Logger log = LoggerFactory.getLogger(ItinerarioIaClienteService.class);

    static final int MAX_REINTENTOS_IA = 3;

    static final String SYSTEM_MESSAGE = "Eres un planificador turístico experto en Costa Rica para "
            + "la aplicación EcoRuta. Generas itinerarios de viaje realistas, agrupando actividades "
            + "geográficamente cercanas y minimizando desplazamientos innecesarios entre provincias. "
            + "Todos los destinos deben estar dentro de Costa Rica. Respondes únicamente con la "
            + "estructura solicitada, sin texto adicional. Reglas estrictas de formato para cada campo:\n"
            + "- horario: SOLO la hora de inicio de la actividad, en formato HH:mm de 24 horas "
            + "(ejemplo: \"09:00\"). Nunca un rango ni la hora de fin; la duración ya va en "
            + "duracionMinutos.\n"
            + "- provincia: exactamente uno de estos 7 valores, sin acentos ni texto adicional: "
            + "SAN_JOSE, ALAJUELA, CARTAGO, HEREDIA, GUANACASTE, PUNTARENAS, LIMON.\n"
            + "- moneda: exactamente \"CRC\" o \"USD\" (solo si costoAproximado no es nulo).\n"
            + "- puntuacionAmbientalPreliminar: un entero entre 0 y 100 (0 el peor, 100 el mejor "
            + "desempeño ambiental), nunca una escala distinta.\n"
            + "- puntuacionAmbientalEstimada (por actividad): un entero entre 0 y 100 estimando "
            + "qué tan sostenible es la actividad/establecimiento. Considera: si es naturaleza/parque "
            + "nacional (alto), si usa recursos naturales de forma responsable (medio-alto), si es "
            + "transporte motorizado o alta huella (bajo). Siempre incluir este campo.\n"
            + "- categoriaTuristica (por actividad): exactamente uno de estos valores: "
            + "NATURALEZA, VIDA_SILVESTRE, AVENTURA, GASTRONOMIA_LOCAL, CULTURA, PLAYAS, BIENESTAR, "
            + "DEPORTES_EXTREMOS, HISTORIA. Asignar según la naturaleza de la actividad.";

    private final ChatClient chatClient;
    private final ItinerarioValidador validador;
    private final String geminiApiKey;

    public ItinerarioIaClienteService(ChatClient.Builder chatClientBuilder,
                                       ItinerarioValidador validador,
                                       @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.validador = validador;
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Genera un itinerario, reintentando hasta {@value #MAX_REINTENTOS_IA} veces si la respuesta
     * llega pero es estructuralmente inválida.
     *
     * @throws ApiException {@code itinerarioGeneracionTimeout()} si la llamada en sí falla (timeout,
     *                       red, proveedor no disponible), sin reintento; {@code
     *                       itinerarioRespuestaInvalida()} si se agotan los reintentos.
     */
    public ResultadoGeneracionIA generar(String promptUsuario, int cantidadDiasSolicitados) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.error("GEMINI_API_KEY no está configurada");
            throw ApiException.itinerarioGeneracionTimeout();
        }

        for (int intento = 1; intento <= MAX_REINTENTOS_IA; intento++) {
            ItinerarioIaResponseDTO respuesta = invocarChatClient(promptUsuario);
            ResultadoValidacionItinerario resultado = validador.validar(respuesta, cantidadDiasSolicitados);

            if (resultado != ResultadoValidacionItinerario.INVALIDO) {
                return new ResultadoGeneracionIA(respuesta, resultado);
            }
            log.warn("Respuesta de IA invalida al generar itinerario (intento {}/{})",
                    intento, MAX_REINTENTOS_IA);
        }
        throw ApiException.itinerarioRespuestaInvalida();
    }

    private ItinerarioIaResponseDTO invocarChatClient(String promptUsuario) {
        try {
            return chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(promptUsuario)
                    .call()
                    .entity(ItinerarioIaResponseDTO.class);
        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini para generar itinerario: {}", e.getMessage(), e);
            throw ApiException.itinerarioGeneracionTimeout();
        }
    }

    public record ResultadoGeneracionIA(
            ItinerarioIaResponseDTO respuesta,
            ResultadoValidacionItinerario resultado
    ) {}
}
