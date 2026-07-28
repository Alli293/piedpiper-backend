package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnvioCorreoVerificacionServiceTest {

    @Test
    void envioExitosoNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        EmailVerificacionService email = (nombre, destinatario, token) -> {
            intentos.incrementAndGet();
            latch.countDown();
        };
        EnvioCorreoVerificacionService envio = new EnvioCorreoVerificacionService(email, 5);

        envio.enviar("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void envioFallidoReintentaHastaTresVeces() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        EmailVerificacionService email = (nombre, destinatario, token) -> {
            latch.countDown();
            throw new IllegalStateException("smtp caido");
        };
        EnvioCorreoVerificacionService envio = new EnvioCorreoVerificacionService(email, 5);

        envio.enviar("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void reintentoExitosoDetieneLosSiguientes() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch latchTercerIntento = new CountDownLatch(1);
        EmailVerificacionService email = (nombre, destinatario, token) -> {
            int intento = intentos.incrementAndGet();
            latch.countDown();
            if (intento >= 3) {
                latchTercerIntento.countDown();
            }
            if (intento == 1) {
                throw new IllegalStateException("smtp caido");
            }
        };
        EnvioCorreoVerificacionService envio = new EnvioCorreoVerificacionService(email, 5);

        envio.enviar("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(intentos.get()).isEqualTo(2);
        assertThat(latchTercerIntento.await(200, TimeUnit.MILLISECONDS)).isFalse();
    }
}
