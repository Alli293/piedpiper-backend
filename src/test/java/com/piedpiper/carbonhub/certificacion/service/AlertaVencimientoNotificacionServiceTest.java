package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.dtos.AlertaVencimientoNotificacionDTO;
import com.piedpiper.carbonhub.notification.service.EmailAlertaVencimientoService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaVencimientoNotificacionServiceTest {

    private static final UUID ALERTA_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 10, 27);

    @Mock
    private AlertaVencimientoDatosService alertaVencimientoDatosService;
    @Mock
    private AlertaEstadoEnvioService alertaEstadoEnvioService;
    @Mock
    private EmailAlertaVencimientoService emailAlertaVencimientoService;

    @InjectMocks
    private AlertaVencimientoNotificacionService service;

    @Test
    void enviaElCorreoConLosDatosDeLaAlertaYLaMarcaEnviada() {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID)).thenReturn(Optional.of(datos("contacto@acme.cr")));

        service.notificar(ALERTA_ID);

        verify(emailAlertaVencimientoService).enviarAlertaVencimiento(
                "contacto@acme.cr",
                "Acme S.A.",
                "Carbono Neutral",
                VENCIMIENTO,
                90L,
                "http://localhost:4200/empresa/acme-sa/reputacion/certificaciones");
        verify(alertaEstadoEnvioService).marcarEnviada(ALERTA_ID);
        verify(alertaEstadoEnvioService, never()).registrarFallo(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "dos@@arrobas.cr", "sin punto@dominio", "@solo-dominio.cr",
            "espacio en@medio.cr", "termina@en.punto."})
    void unCorreoConFormatoInvalidoOmiteElEnvioYDejaLaAlertaFallida(String correo) {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID)).thenReturn(Optional.of(datos(correo)));

        service.notificar(ALERTA_ID);

        verifyNoInteractions(emailAlertaVencimientoService);
        verify(alertaEstadoEnvioService).marcarFallidaSinReintento(ALERTA_ID);
        verify(alertaEstadoEnvioService, never()).marcarEnviada(any());
    }

    @Test
    void unCorreoNuloOmiteElEnvioYDejaLaAlertaFallida() {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID)).thenReturn(Optional.of(datos(null)));

        service.notificar(ALERTA_ID);

        verifyNoInteractions(emailAlertaVencimientoService);
        verify(alertaEstadoEnvioService).marcarFallidaSinReintento(ALERTA_ID);
    }

    @Test
    void unFalloDelServicioDeCorreoRegistraElIntentoSinMarcarlaEnviada() {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID)).thenReturn(Optional.of(datos("contacto@acme.cr")));
        doThrow(new IllegalStateException("SMTP caido"))
                .when(emailAlertaVencimientoService).enviarAlertaVencimiento(
                        anyString(), anyString(), anyString(), any(), anyLong(), anyString());

        service.notificar(ALERTA_ID);

        verify(alertaEstadoEnvioService).registrarFallo(ALERTA_ID);
        verify(alertaEstadoEnvioService, never()).marcarEnviada(any());
    }

    @Test
    void unaAlertaQueYaNoExisteNoEnviaNiEscribeEstado() {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID)).thenReturn(Optional.empty());

        service.notificar(ALERTA_ID);

        verifyNoInteractions(emailAlertaVencimientoService);
        verifyNoInteractions(alertaEstadoEnvioService);
    }

    @Test
    void unCorreoConSubdominiosSeConsideraValido() {
        when(alertaVencimientoDatosService.datosDe(ALERTA_ID))
                .thenReturn(Optional.of(datos("contacto@mail.acme.co.cr")));

        service.notificar(ALERTA_ID);

        verify(emailAlertaVencimientoService).enviarAlertaVencimiento(
                eq("contacto@mail.acme.co.cr"), anyString(), anyString(), any(), anyLong(), anyString());
        verify(alertaEstadoEnvioService).marcarEnviada(ALERTA_ID);
    }

    private static AlertaVencimientoNotificacionDTO datos(String correo) {
        return new AlertaVencimientoNotificacionDTO(
                ALERTA_ID,
                correo,
                "Acme S.A.",
                "Carbono Neutral",
                VENCIMIENTO,
                90L,
                "90_dias",
                "http://localhost:4200/empresa/acme-sa/reputacion/certificaciones");
    }
}
