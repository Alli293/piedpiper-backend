package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditoria.models.events.AuditoriaFinalizadaEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MetricasReputacionAuditorListener {

    private static final Logger log =
            LoggerFactory.getLogger(MetricasReputacionAuditorListener.class);

    private final MetricasReputacionAuditorService metricasReputacionAuditorService;

    public MetricasReputacionAuditorListener(
            MetricasReputacionAuditorService metricasReputacionAuditorService) {
        this.metricasReputacionAuditorService = metricasReputacionAuditorService;
    }

    @EventListener
    public void alFinalizarAuditoria(AuditoriaFinalizadaEvent event) {
        try {
            metricasReputacionAuditorService.recalcular(event.auditorId());
        } catch (Exception e) {
            log.error("No se pudieron recalcular las metricas de reputacion del auditor {} "
                    + "por la auditoria {}.", event.auditorId(), event.auditoriaId(), e);
        }
    }
}
