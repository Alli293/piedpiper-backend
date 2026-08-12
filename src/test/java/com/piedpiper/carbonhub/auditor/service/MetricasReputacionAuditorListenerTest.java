package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditoria.models.events.AuditoriaFinalizadaEvent;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MetricasReputacionAuditorListenerTest {

    @Test
    void listenerInvocaServicioAlRecibirEvento() {
        MetricasReputacionAuditorService service = mock(MetricasReputacionAuditorService.class);
        MetricasReputacionAuditorListener listener = new MetricasReputacionAuditorListener(service);
        AuditoriaFinalizadaEvent event = event();

        listener.alFinalizarAuditoria(event);

        verify(service).recalcular(event.auditorId());
    }

    @Test
    void siServicioFallaListenerNoPropagaExcepcion() {
        MetricasReputacionAuditorService service = mock(MetricasReputacionAuditorService.class);
        MetricasReputacionAuditorListener listener = new MetricasReputacionAuditorListener(service);
        AuditoriaFinalizadaEvent event = event();
        doThrow(new IllegalStateException("fallo simulado"))
                .when(service).recalcular(event.auditorId());

        assertThatNoException().isThrownBy(() -> listener.alFinalizarAuditoria(event));
    }

    private AuditoriaFinalizadaEvent event() {
        return new AuditoriaFinalizadaEvent(
                UUID.randomUUID(),
                UUID.randomUUID());
    }
}
