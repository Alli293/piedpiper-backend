package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

/**
 * Evalua una unica certificacion contra los umbrales de vencimiento (90, 30 y
 * 7 dias) y registra la alerta correspondiente si corresponde. Invocada por
 * {@link AlertaVencimientoSchedulerService} una vez por certificacion activa.
 */
@Service
public class AlertaVencimientoEvaluacionService {

    private static final Logger log = LoggerFactory.getLogger(AlertaVencimientoEvaluacionService.class);

    private final AlertaRepository alertaRepository;

    public AlertaVencimientoEvaluacionService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public void evaluar(Certificacion certificacion) {
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), certificacion.getFechaVencimiento());

        Optional<TipoAlerta> tipoAlerta = Arrays.stream(TipoAlerta.values())
                .filter(tipo -> tipo.getDias() == diasRestantes)
                .findFirst();

        if (tipoAlerta.isEmpty()) {
            return;
        }

        if (alertaRepository.existsByCertificacionIdAndTipoAlerta(certificacion.getId(), tipoAlerta.get())) {
            return;
        }

        alertaRepository.save(Alerta.builder()
                .empresa(certificacion.getEmpresa())
                .certificacion(certificacion)
                .tipoAlerta(tipoAlerta.get())
                .estado(EstadoAlerta.PENDIENTE)
                .fechaGeneracion(Instant.now())
                .build());

        log.info("Alerta {} generada para la certificacion {}", tipoAlerta.get(), certificacion.getId());
    }
}
