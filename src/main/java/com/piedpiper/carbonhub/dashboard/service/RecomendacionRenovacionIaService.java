package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.dashboard.models.dtos.CertAlertaDTO;
import com.piedpiper.carbonhub.dashboard.models.dtos.RecomendacionIaTexto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Encapsula ÚNICAMENTE la llamada a Gemini para redactar la justificación
 * y la sugerencia de acción de la recomendación de renovación (PP-72). Cuál
 * certificación es prioritaria ya lo decidió
 * {@link RecomendacionRenovacionSeleccionService} de forma determinista —
 * la IA solo pone en palabras una decisión ya tomada, nunca elige el orden.
 *
 * <p>Cualquier fallo (API key ausente, timeout, respuesta incompleta) se
 * traduce a {@link Optional#empty()} sin propagar la excepción: el
 * criterio de aceptación pide que el bloque muestre un mensaje de "no
 * disponible" en vez de romper el resto del dashboard.</p>
 */
@Service
public class RecomendacionRenovacionIaService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacionRenovacionIaService.class);

    static final String SYSTEM_MESSAGE = "Eres un asesor de sostenibilidad para la aplicación "
            + "CarbonHub. Respondes en español de Costa Rica, en tono claro, profesional y directo. "
            + "Devuelve una justificación de 2 a 4 oraciones explicando por qué esta certificación "
            + "debe renovarse primero, basándote solo en los días restantes y el impacto en huella "
            + "que se te dan, y una única sugerencia de acción concreta y accionable. No inventes "
            + "cifras ni datos que no estén en el mensaje del usuario.";

    private final ChatClient chatClient;
    private final String geminiApiKey;

    public RecomendacionRenovacionIaService(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.geminiApiKey = geminiApiKey;
    }

    public Optional<RecomendacionIaTexto> generar(CertAlertaDTO prioritaria) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.error("GEMINI_API_KEY no está configurada");
            return Optional.empty();
        }

        try {
            RecomendacionIaTexto resultado = chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(construirPromptUsuario(prioritaria))
                    .call()
                    .entity(RecomendacionIaTexto.class);

            if (resultado == null
                    || esVacio(resultado.justificacion())
                    || esVacio(resultado.sugerenciaAccion())) {
                log.warn("Respuesta de IA inválida o incompleta para la recomendación de renovación");
                return Optional.empty();
            }

            return Optional.of(resultado);
        } catch (Exception e) {
            log.warn("Error generando la recomendación de renovación vía IA: {}", e.getMessage());
            return Optional.empty();
        }
    }

    String construirPromptUsuario(CertAlertaDTO prioritaria) {
        StringBuilder sb = new StringBuilder();
        sb.append("Certificación: ").append(prioritaria.getNombreCertificacion()).append("\n");
        sb.append("Días restantes hasta el vencimiento: ").append(prioritaria.getDiasRestantes()).append("\n");
        sb.append("Impacto en huella verificada (t CO2e): ").append(prioritaria.getImpactoHuellaT()).append("\n");
        return sb.toString();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
