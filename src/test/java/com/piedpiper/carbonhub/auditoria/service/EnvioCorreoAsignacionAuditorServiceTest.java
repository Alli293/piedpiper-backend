package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.notification.service.EmailAsignacionAuditorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EnvioCorreoAsignacionAuditorServiceTest {

    @Mock
    private EmailAsignacionAuditorService emailAsignacionAuditorService;

    private EnvioCorreoAsignacionAuditorService service;

    @BeforeEach
    void configurar() {
        service = new EnvioCorreoAsignacionAuditorService(emailAsignacionAuditorService);
    }

    @Test
    void envioExitosoNoDejaCorreosPendientesDeReintento() {
        service.enviarAsignacion("Ana Auditora", "auditora@carbonhub.cr", "Empresa Demo");
        service.reintentarPendientes();

        verify(emailAsignacionAuditorService)
                .enviarAsignacion("Ana Auditora", "auditora@carbonhub.cr", "Empresa Demo");
        verifyNoMoreInteractions(emailAsignacionAuditorService);
    }

    @Test
    void unEnvioQueFallaSeReintentaHastaTresVecesYLuegoSeDescarta() {
        doThrow(new IllegalStateException("SMTP caido"))
                .when(emailAsignacionAuditorService).enviarAsignacion(anyString(), anyString(), anyString());

        assertThatCode(() -> service.enviarAsignacion("Ana Auditora", "auditora@carbonhub.cr", "Empresa Demo"))
                .doesNotThrowAnyException();
        service.reintentarPendientes();
        service.reintentarPendientes();
        service.reintentarPendientes();
        service.reintentarPendientes();

        verify(emailAsignacionAuditorService, times(4))
                .enviarAsignacion("Ana Auditora", "auditora@carbonhub.cr", "Empresa Demo");
    }

    @Test
    void elCorreoDeExpiracionTambienSeReintentaCuandoFalla() {
        doThrow(new IllegalStateException("SMTP caido"))
                .doNothing()
                .when(emailAsignacionAuditorService)
                .enviarExpiracionAsignacion(anyString(), anyString(), anyString());

        service.enviarExpiracion("contacto@empresa.cr", "Empresa Demo", "Ana Auditora");
        service.reintentarPendientes();
        service.reintentarPendientes();

        verify(emailAsignacionAuditorService, times(2))
                .enviarExpiracionAsignacion("contacto@empresa.cr", "Empresa Demo", "Ana Auditora");
    }
}
