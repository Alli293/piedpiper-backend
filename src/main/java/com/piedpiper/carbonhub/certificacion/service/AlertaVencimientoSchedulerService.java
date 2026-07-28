package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Proceso nocturno (PP-70): evalua a diario todas las certificaciones activas
 * y genera las alertas de vencimiento a 90, 30 y 7 dias que correspondan. Un
 * error al evaluar una certificacion no detiene el procesamiento de las
 * demas; se registra en el log y se continua.
 */
@Service
public class AlertaVencimientoSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AlertaVencimientoSchedulerService.class);

    private final CertificacionRepository certificacionRepository;
    private final AlertaVencimientoEvaluacionService alertaVencimientoEvaluacionService;

    public AlertaVencimientoSchedulerService(
            CertificacionRepository certificacionRepository,
            AlertaVencimientoEvaluacionService alertaVencimientoEvaluacionService) {
        this.certificacionRepository = certificacionRepository;
        this.alertaVencimientoEvaluacionService = alertaVencimientoEvaluacionService;
    }

    @Scheduled(cron = "${certificacion.alertas.cron-vencimiento:0 0 2 * * *}")
    public void evaluarVencimientos() {
        List<Certificacion> activas = certificacionRepository.findByEstado(EstadoCertificacion.ACTIVA);
        log.info("Evaluando vencimiento de {} certificaciones activas", activas.size());

        for (Certificacion certificacion : activas) {
            try {
                alertaVencimientoEvaluacionService.evaluar(certificacion);
            } catch (Exception e) {
                log.error("Error al evaluar el vencimiento de la certificacion {}", certificacion.getId(), e);
            }
        }
    }
}
