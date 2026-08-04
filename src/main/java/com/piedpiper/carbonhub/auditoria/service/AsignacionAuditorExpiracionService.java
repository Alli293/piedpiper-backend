package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AsignacionAuditorExpiracionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionAuditorExpiracionService.class);

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final AsignacionAuditorLiberacionService asignacionAuditorLiberacionService;
    private final long horasSinRespuesta;

    public AsignacionAuditorExpiracionService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            AsignacionAuditorLiberacionService asignacionAuditorLiberacionService,
            @Value("${auditoria.expiracion-asignacion-horas:120}") long horasSinRespuesta) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.asignacionAuditorLiberacionService = asignacionAuditorLiberacionService;
        this.horasSinRespuesta = horasSinRespuesta;
    }

    @Scheduled(fixedDelayString = "${auditoria.expiracion-intervalo-ms:3600000}")
    public void liberarAsignacionesSinRespuesta() {
        Instant limite = Instant.now().minus(horasSinRespuesta, ChronoUnit.HOURS);
        solicitudAuditoriaRepository
                .idsConAsignacionVencida(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, limite)
                .forEach(this::liberarAislada);
    }

    private void liberarAislada(UUID solicitudId) {
        try {
            asignacionAuditorLiberacionService.liberar(solicitudId);
        } catch (RuntimeException e) {
            log.error("No se pudo liberar la asignacion vencida de la solicitud {}", solicitudId, e);
        }
    }
}
