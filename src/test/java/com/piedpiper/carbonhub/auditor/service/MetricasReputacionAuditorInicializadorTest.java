package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricasReputacionAuditorInicializadorTest {

    @Test
    void recalculaMetricasExistentesAlArrancar() {
        PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        MetricasReputacionAuditorService service = mock(MetricasReputacionAuditorService.class);
        UUID auditorUno = UUID.randomUUID();
        UUID auditorDos = UUID.randomUUID();
        when(perfilAuditorRepository.listarAuditorIdsConPerfil())
                .thenReturn(List.of(auditorUno, auditorDos));
        MetricasReputacionAuditorInicializador inicializador =
                new MetricasReputacionAuditorInicializador(perfilAuditorRepository, service);

        inicializador.recalcularMetricasExistentes();

        verify(service).recalcular(auditorUno);
        verify(service).recalcular(auditorDos);
    }

    @Test
    void falloDeUnAuditorNoInterrumpeSincronizacionDelResto() {
        PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        MetricasReputacionAuditorService service = mock(MetricasReputacionAuditorService.class);
        UUID auditorConFallo = UUID.randomUUID();
        UUID auditorSiguiente = UUID.randomUUID();
        when(perfilAuditorRepository.listarAuditorIdsConPerfil())
                .thenReturn(List.of(auditorConFallo, auditorSiguiente));
        doThrow(new IllegalStateException("fallo simulado"))
                .when(service).recalcular(auditorConFallo);
        MetricasReputacionAuditorInicializador inicializador =
                new MetricasReputacionAuditorInicializador(perfilAuditorRepository, service);

        assertThatNoException().isThrownBy(inicializador::recalcularMetricasExistentes);

        verify(service).recalcular(auditorSiguiente);
    }
}
