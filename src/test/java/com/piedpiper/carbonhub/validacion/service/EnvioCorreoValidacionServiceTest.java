package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.notification.service.EmailValidacionAuditorService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class EnvioCorreoValidacionServiceTest {

    @Test
    void reintentaElEnvioTrasUnFalloTransitorio() {
        EmailValidacionAuditorService email = mock(EmailValidacionAuditorService.class);
        doThrow(new RuntimeException("smtp caido"))
                .doNothing()
                .when(email).enviarResultadoValidacion(any(), any(), anyBoolean(), any());

        EnvioCorreoValidacionService service = new EnvioCorreoValidacionService(email, 0L);
        try {
            service.enviar("Ana Mora", "ana@correo.com", true, null);

            verify(email, timeout(2000).times(2))
                    .enviarResultadoValidacion(eq("Ana Mora"), eq("ana@correo.com"), eq(true), isNull());
        } finally {
            service.cerrar();
        }
    }

    @Test
    void noReintentaCuandoElEnvioEsExitoso() {
        EmailValidacionAuditorService email = mock(EmailValidacionAuditorService.class);
        doNothing().when(email).enviarResultadoValidacion(any(), any(), anyBoolean(), any());

        EnvioCorreoValidacionService service = new EnvioCorreoValidacionService(email, 0L);
        try {
            service.enviar("Ana Mora", "ana@correo.com", false, "Documentos ilegibles");

            verify(email, timeout(500).times(1))
                    .enviarResultadoValidacion(any(), any(), anyBoolean(), any());
        } finally {
            service.cerrar();
        }
    }
}
