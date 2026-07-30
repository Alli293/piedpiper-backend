package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaEstadoEnvioServiceTest {

    private static final UUID ALERTA_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private AlertaEstadoEnvioService service;

    @Test
    void marcarEnviadaGuardaEstadoFechaDeEnvioYSumaElIntento() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.of(alerta(EstadoAlerta.PENDIENTE, 0)));

        service.marcarEnviada(ALERTA_ID);

        Alerta guardada = capturar();
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.ENVIADA);
        assertThat(guardada.getFechaEnvio()).isNotNull();
        assertThat(guardada.getIntentosEnvio()).isEqualTo(1);
    }

    @Test
    void unFalloConIntentosRestantesSumaElIntentoYLaDejaPendiente() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.of(alerta(EstadoAlerta.PENDIENTE, 0)));

        service.registrarFallo(ALERTA_ID);

        Alerta guardada = capturar();
        assertThat(guardada.getIntentosEnvio()).isEqualTo(1);
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.PENDIENTE);
        assertThat(guardada.getFechaEnvio()).isNull();
    }

    @Test
    void elTercerFalloAgotaLosIntentosYLaDejaFallida() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.of(alerta(EstadoAlerta.PENDIENTE, 2)));

        service.registrarFallo(ALERTA_ID);

        Alerta guardada = capturar();
        assertThat(guardada.getIntentosEnvio()).isEqualTo(AlertaEstadoEnvioService.INTENTOS_MAXIMOS);
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.FALLIDA);
    }

    @Test
    void elSegundoFalloTodaviaNoLaDejaFallida() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.of(alerta(EstadoAlerta.PENDIENTE, 1)));

        service.registrarFallo(ALERTA_ID);

        Alerta guardada = capturar();
        assertThat(guardada.getIntentosEnvio()).isEqualTo(2);
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.PENDIENTE);
    }

    @Test
    void marcarFallidaSinReintentoAgotaLosIntentosDeUnaVez() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.of(alerta(EstadoAlerta.PENDIENTE, 0)));

        service.marcarFallidaSinReintento(ALERTA_ID);

        Alerta guardada = capturar();
        assertThat(guardada.getEstado()).isEqualTo(EstadoAlerta.FALLIDA);
        assertThat(guardada.getIntentosEnvio()).isEqualTo(AlertaEstadoEnvioService.INTENTOS_MAXIMOS);
        assertThat(guardada.getFechaEnvio()).isNull();
    }

    @Test
    void unaAlertaInexistenteNoGuardaNada() {
        when(alertaRepository.findById(ALERTA_ID)).thenReturn(Optional.empty());

        service.marcarEnviada(ALERTA_ID);
        service.registrarFallo(ALERTA_ID);
        service.marcarFallidaSinReintento(ALERTA_ID);

        verify(alertaRepository, never()).save(any());
    }

    private Alerta capturar() {
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Alerta alerta(EstadoAlerta estado, int intentos) {
        return Alerta.builder()
                .id(ALERTA_ID)
                .tipoAlerta(TipoAlerta.DIAS_90)
                .estado(estado)
                .intentosEnvio(intentos)
                .fechaGeneracion(Instant.now())
                .build();
    }
}
