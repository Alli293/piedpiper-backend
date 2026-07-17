package com.piedpiper.carbonhub.ima.service;

import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImaInterpretacionService {

    private static final Logger log = LoggerFactory.getLogger(ImaInterpretacionService.class);

    private final ChatClient chatClient;
    private final ImaSnapshotRepository imaSnapshotRepository;

    public ImaInterpretacionService(ChatClient.Builder chatClientBuilder,
                                    ImaSnapshotRepository imaSnapshotRepository) {
        this.chatClient = chatClientBuilder.build();
        this.imaSnapshotRepository = imaSnapshotRepository;
    }

    @Async
    @Transactional
    public void generarInterpretacion(ImaSnapshot snapshot) {
        try {
            String prompt = construirPrompt(snapshot);
            String interpretacion = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            snapshot.setInterpretacionIa(interpretacion);
            imaSnapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.warn("No se pudo generar la interpretación IA para snapshot {}: {}",
                    snapshot.getId(), e.getMessage());
        }
    }

    private String construirPrompt(ImaSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un consultor de sostenibilidad corporativa. ")
                .append("Genera una interpretación breve (máximo 3 oraciones) del siguiente Índice de Madurez Ambiental (IMA) ")
                .append("para una empresa. Usa un tono profesional y orientado a la acción.\n\n")
                .append("Período: ").append(snapshot.getMes()).append("/").append(snapshot.getAnio()).append("\n")
                .append("Cobertura: ").append(snapshot.getCobertura()).append("/100\n")
                .append("Consistencia: ").append(snapshot.getConsistencia()).append("/100\n");

        if (snapshot.getPuntajeIntensidadSectorial() != null) {
            sb.append("Puntaje de intensidad sectorial: ")
                    .append(snapshot.getPuntajeIntensidadSectorial()).append("/100\n");
        } else {
            sb.append("Puntaje de intensidad sectorial: No disponible (datos insuficientes)\n");
        }

        sb.append("IMA total: ").append(snapshot.getIma()).append("/100\n");

        if (snapshot.isParcial()) {
            sb.append("Nota: El IMA es parcial. ").append(snapshot.getMotivoParcial()).append("\n");
        }

        return sb.toString();
    }
}
