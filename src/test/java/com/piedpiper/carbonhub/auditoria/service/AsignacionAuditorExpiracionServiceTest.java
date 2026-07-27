package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionAuditorExpiracionServiceTest {

    private static final long HORAS_SIN_RESPUESTA = 120;
    private static final UUID VENCIDA_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID OTRA_VENCIDA_ID = UUID.fromString("7b2d1b7f-69c3-4e29-8e4f-4b5c6d7e8f90");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private AsignacionAuditorLiberacionService asignacionAuditorLiberacionService;

    @Test
    void liberaLasSolicitudesAsignadasHaceMasDeCientoVeinteHorasSinRespuesta() {
        when(solicitudAuditoriaRepository.idsConAsignacionVencida(any(), any()))
                .thenReturn(List.of(VENCIDA_ID, OTRA_VENCIDA_ID));
        Instant antesDeEjecutar = Instant.now();

        servicio().liberarAsignacionesSinRespuesta();

        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(solicitudAuditoriaRepository).idsConAsignacionVencida(
                eq(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA), limite.capture());
        assertThat(limite.getValue()).isBetween(
                antesDeEjecutar.minus(HORAS_SIN_RESPUESTA, ChronoUnit.HOURS),
                Instant.now().minus(HORAS_SIN_RESPUESTA, ChronoUnit.HOURS));

        verify(asignacionAuditorLiberacionService).liberar(VENCIDA_ID);
        verify(asignacionAuditorLiberacionService).liberar(OTRA_VENCIDA_ID);
    }

    @Test
    void noTocaLasSolicitudesQueNoAlcanzaronElUmbral() {
        when(solicitudAuditoriaRepository.idsConAsignacionVencida(any(), any())).thenReturn(List.of());

        servicio().liberarAsignacionesSinRespuesta();

        verify(asignacionAuditorLiberacionService, never()).liberar(any());
    }

    @Test
    void unFalloAlLiberarUnaSolicitudNoImpideLiberarLasRestantes() {
        when(solicitudAuditoriaRepository.idsConAsignacionVencida(any(), any()))
                .thenReturn(List.of(VENCIDA_ID, OTRA_VENCIDA_ID));
        doThrow(new IllegalStateException("fallo al liberar"))
                .when(asignacionAuditorLiberacionService).liberar(VENCIDA_ID);

        assertThatCode(() -> servicio().liberarAsignacionesSinRespuesta()).doesNotThrowAnyException();

        verify(asignacionAuditorLiberacionService).liberar(OTRA_VENCIDA_ID);
    }

    @Test
    void cadaSolicitudSeLiberaEnUnaTransaccionIndependiente() throws NoSuchMethodException {
        Method liberar = AsignacionAuditorLiberacionService.class.getMethod("liberar", UUID.class);

        Transactional transaccional = liberar.getAnnotation(Transactional.class);

        assertThat(transaccional).isNotNull();
        assertThat(transaccional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private AsignacionAuditorExpiracionService servicio() {
        return new AsignacionAuditorExpiracionService(
                solicitudAuditoriaRepository, asignacionAuditorLiberacionService, HORAS_SIN_RESPUESTA);
    }
}
