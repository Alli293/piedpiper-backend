package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaPendienteReintentoServiceTest {

    private static final UUID PRIMERA = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID SEGUNDA = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Mock
    private AlertaRepository alertaRepository;
    @Mock
    private AlertaVencimientoNotificacionService alertaVencimientoNotificacionService;

    @InjectMocks
    private AlertaPendienteReintentoService service;

    @Test
    void reintentaTodasLasAlertasPendientes() {
        pendientes(List.of(alerta(PRIMERA), alerta(SEGUNDA)));

        service.reintentarPendientes();

        verify(alertaVencimientoNotificacionService).notificar(PRIMERA);
        verify(alertaVencimientoNotificacionService).notificar(SEGUNDA);
    }

    @Test
    void soloBuscaPendientesQueNoAgotaronLosIntentos() {
        pendientes(List.of());

        service.reintentarPendientes();

        verify(alertaRepository).findByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(
                EstadoAlerta.PENDIENTE, AlertaEstadoEnvioService.INTENTOS_MAXIMOS);
        verifyNoInteractions(alertaVencimientoNotificacionService);
    }

    @Test
    void unFalloEnUnaAlertaNoDetieneElResto() {
        pendientes(List.of(alerta(PRIMERA), alerta(SEGUNDA)));
        doThrow(new IllegalStateException("base caida"))
                .when(alertaVencimientoNotificacionService).notificar(PRIMERA);

        assertThatCode(() -> service.reintentarPendientes()).doesNotThrowAnyException();

        verify(alertaVencimientoNotificacionService).notificar(SEGUNDA);
    }

    private void pendientes(List<Alerta> alertas) {
        when(alertaRepository.findByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(
                any(EstadoAlerta.class), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(alertas);
    }

    private static Alerta alerta(UUID id) {
        return Alerta.builder()
                .id(id)
                .tipoAlerta(TipoAlerta.DIAS_30)
                .estado(EstadoAlerta.PENDIENTE)
                .fechaGeneracion(Instant.now())
                .build();
    }
}
