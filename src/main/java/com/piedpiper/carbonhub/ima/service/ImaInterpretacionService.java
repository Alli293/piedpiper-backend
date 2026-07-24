package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.dtos.InterpretacionIma;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImaInterpretacionService {

    private static final Logger log = LoggerFactory.getLogger(ImaInterpretacionService.class);

    static final String NO_DISPONIBLE = "No disponible";
    static final String SYSTEM_MESSAGE = "Eres un asesor de sostenibilidad. Responde en español de Costa Rica, "
            + "en tono claro y profesional. Devuelve solo una interpretación de 2 a 4 oraciones y un único "
            + "siguiente paso accionable. No inventes cifras que no estén en los datos.";

    private final ChatClient chatClient;
    private final ImaSnapshotRepository imaSnapshotRepository;
    private final EmpresaRepository empresaRepository;
    private final String geminiApiKey;

    public ImaInterpretacionService(ChatClient.Builder chatClientBuilder,
                                    ImaSnapshotRepository imaSnapshotRepository,
                                    EmpresaRepository empresaRepository,
                                    @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.imaSnapshotRepository = imaSnapshotRepository;
        this.empresaRepository = empresaRepository;
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Genera y persiste la interpretación del IMA usando ChatClient (Gemini).
     * Se ejecuta fuera de cualquier transacción activa para evitar mantener
     * conexiones durante la llamada HTTP externa.
     * Cualquier fallo resulta en "No disponible" sin afectar el IMA.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generarInterpretacion(ImaSnapshot snapshot, String sectorNombre,
                                       AgregadoSectorial agregado, String tendencia) {
        try {
            // 1. Verificar API key configurada
            if (geminiApiKey == null || geminiApiKey.isBlank()) {
                log.error("GEMINI_API_KEY no está configurada");
                persistirNoDisponible(snapshot);
                return;
            }

            // 2. Construir prompt
            String promptUsuario = construirPromptUsuario(snapshot, sectorNombre, agregado, tendencia);

            // 3. Verificar privacidad — obtener datos de la empresa para validación
            Empresa empresa = empresaRepository.findById(snapshot.getEmpresaId()).orElse(null);
            if (empresa != null) {
                boolean privacidadOk = verificarPrivacidad(
                        promptUsuario,
                        empresa.getNombreEmpresa(),
                        empresa.getId(),
                        empresa.getCantidadEmpleados()
                );
                if (!privacidadOk) {
                    log.error("Verificación de privacidad fallida: el prompt contiene datos sensibles");
                    persistirNoDisponible(snapshot);
                    return;
                }
            }

            // 4. Invocar ChatClient con respuesta estructurada
            InterpretacionIma resultado = chatClient.prompt()
                    .system(SYSTEM_MESSAGE)
                    .user(promptUsuario)
                    .call()
                    .entity(InterpretacionIma.class);

            // 5. Validar respuesta
            if (resultado == null
                    || resultado.interpretacion() == null || resultado.interpretacion().isBlank()
                    || resultado.siguientePaso() == null || resultado.siguientePaso().isBlank()) {
                log.warn("Respuesta del ChatClient inválida o con campos vacíos para snapshot {}",
                        snapshot.getId());
                persistirNoDisponible(snapshot);
                return;
            }

            // 6. Persistir resultado válido
            snapshot.setInterpretacion(resultado.interpretacion());
            snapshot.setSiguientePaso(resultado.siguientePaso());
            imaSnapshotRepository.save(snapshot);

        } catch (Exception e) {
            log.warn("Error generando interpretación IA para snapshot {}: {}",
                    snapshot.getId(), e.getMessage());
            persistirNoDisponible(snapshot);
        }
    }

    /**
     * Construye el prompt de usuario con datos anonimizados del sector.
     */
    String construirPromptUsuario(ImaSnapshot snapshot, String sectorNombre,
                                   AgregadoSectorial agregado, String tendencia) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sector: ").append(sectorNombre).append("\n");
        sb.append("Puntajes de la empresa (0–100):\n");
        sb.append("  - Cobertura: ").append(snapshot.getCobertura()).append("\n");
        sb.append("  - Puntaje de intensidad sectorial: ")
                .append(snapshot.getPuntajeIntensidadSectorial() != null
                        ? snapshot.getPuntajeIntensidadSectorial()
                        : NO_DISPONIBLE)
                .append("\n");
        sb.append("  - Consistencia: ").append(snapshot.getConsistencia()).append("\n");
        sb.append("  - IMA: ").append(snapshot.getIma()).append("\n");
        sb.append("Promedios del sector (").append(agregado.getCantidadEmpresas()).append(" empresas):\n");
        sb.append("  - Intensidad promedio: ").append(agregado.getIntensidadPromedio()).append("\n");
        sb.append("Tendencia respecto al mes anterior: ").append(tendencia).append("\n");
        return sb.toString();
    }

    /**
     * Verifica que el prompt no contiene datos sensibles de la empresa.
     * Retorna false si se detectan datos sensibles (invocación debe abortarse).
     */
    boolean verificarPrivacidad(String prompt, String nombreEmpresa, UUID empresaId,
                                Integer cantidadEmpleados) {
        if (prompt == null) {
            return true;
        }
        String promptLower = prompt.toLowerCase();

        if (nombreEmpresa != null && !nombreEmpresa.isBlank()
                && promptLower.contains(nombreEmpresa.toLowerCase())) {
            log.error("ALERTA DE SEGURIDAD: Datos sensibles detectados en prompt de IA. Tipo: nombreEmpresa");
            return false;
        }

        if (empresaId != null && promptLower.contains(empresaId.toString().toLowerCase())) {
            log.error("ALERTA DE SEGURIDAD: Datos sensibles detectados en prompt de IA. Tipo: empresaId");
            return false;
        }

        if (cantidadEmpleados != null) {
            String empleadosStr = cantidadEmpleados.toString();
            // Only check if the number is specific enough (>= 3 digits) to avoid false positives
            // with common prompt numbers like scores (0-100) or sector counts
            if (empleadosStr.length() >= 3 && prompt.contains(empleadosStr)) {
                log.error("ALERTA DE SEGURIDAD: Datos sensibles detectados en prompt de IA. Tipo: cantidadEmpleados");
                return false;
            }
        }

        return true;
    }

    private void persistirNoDisponible(ImaSnapshot snapshot) {
        snapshot.setInterpretacion(NO_DISPONIBLE);
        snapshot.setSiguientePaso(NO_DISPONIBLE);
        imaSnapshotRepository.save(snapshot);
    }
}
