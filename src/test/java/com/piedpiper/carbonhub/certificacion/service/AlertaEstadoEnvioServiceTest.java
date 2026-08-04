package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaEstadoEnvioServiceTest {

    private static final UUID ALERTA_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final int MAXIMOS = AlertaEstadoEnvioService.INTENTOS_MAXIMOS;

    @Mock
    private AlertaRepository alertaRepository;

    @InjectMocks
    private AlertaEstadoEnvioService service;

    @Test
    void reclamarDevuelveTrueCuandoLaSentenciaCondicionalTomaLaFila() {
        when(alertaRepository.reclamarParaEnvio(ALERTA_ID, EstadoAlerta.PENDIENTE, MAXIMOS)).thenReturn(1);

        assertThat(service.reclamar(ALERTA_ID)).isTrue();
    }

    @Test
    void reclamarDevuelveFalseCuandoOtroProcesoLaTomoPrimero() {
        when(alertaRepository.reclamarParaEnvio(ALERTA_ID, EstadoAlerta.PENDIENTE, MAXIMOS)).thenReturn(0);

        assertThat(service.reclamar(ALERTA_ID)).isFalse();
    }

    @Test
    void reclamarCondicionaAQueSigaPendienteYLeQuedenIntentos() {
        when(alertaRepository.reclamarParaEnvio(any(), any(), anyInt()))
                .thenReturn(1);

        service.reclamar(ALERTA_ID);

        verify(alertaRepository).reclamarParaEnvio(ALERTA_ID, EstadoAlerta.PENDIENTE, MAXIMOS);
    }

    @Test
    void marcarEnviadaCierraSoloSiSiguePendiente() {
        service.marcarEnviada(ALERTA_ID);

        verify(alertaRepository).marcarEnviada(
                eq(ALERTA_ID), eq(EstadoAlerta.ENVIADA), eq(EstadoAlerta.PENDIENTE), any(Instant.class));
    }

    @Test
    void registrarFalloSoloCierraCuandoYaSeAgotaronLosIntentos() {
        service.registrarFallo(ALERTA_ID);

        verify(alertaRepository).marcarFallidaSiAgotoIntentos(
                ALERTA_ID, EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, MAXIMOS);
    }

    @Test
    void marcarFallidaSinReintentoAgotaLosIntentosDeUnaVez() {
        service.marcarFallidaSinReintento(ALERTA_ID);

        verify(alertaRepository).marcarFallidaDefinitiva(
                ALERTA_ID, EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, MAXIMOS);
    }

    @Test
    void elTopeDeIntentosEsTresComoPideElCriterio() {
        assertThat(AlertaEstadoEnvioService.INTENTOS_MAXIMOS).isEqualTo(3);
    }
}
