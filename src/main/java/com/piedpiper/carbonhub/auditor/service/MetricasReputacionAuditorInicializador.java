package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "app.auditor.metricas",
        name = "sincronizar-al-iniciar",
        havingValue = "true",
        matchIfMissing = true)
public class MetricasReputacionAuditorInicializador {

    private static final Logger log =
            LoggerFactory.getLogger(MetricasReputacionAuditorInicializador.class);

    private final PerfilAuditorRepository perfilAuditorRepository;
    private final MetricasReputacionAuditorService metricasReputacionAuditorService;

    public MetricasReputacionAuditorInicializador(
            PerfilAuditorRepository perfilAuditorRepository,
            MetricasReputacionAuditorService metricasReputacionAuditorService) {
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.metricasReputacionAuditorService = metricasReputacionAuditorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recalcularMetricasExistentes() {
        List<UUID> auditorIds = perfilAuditorRepository.listarAuditorIdsConPerfil();
        auditorIds.forEach(this::recalcularSinInterrumpirArranque);
        log.info("Metricas de reputacion sincronizadas para {} perfiles de auditor.",
                auditorIds.size());
    }

    private void recalcularSinInterrumpirArranque(UUID auditorId) {
        try {
            metricasReputacionAuditorService.recalcular(auditorId);
        } catch (Exception e) {
            log.error("No se pudieron sincronizar las metricas de reputacion del auditor {}.",
                    auditorId, e);
        }
    }
}
