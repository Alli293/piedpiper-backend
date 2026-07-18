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
        EmailVerificacionService email = (nombre, destinatario, token) -> intentos.incrementAndGet();
        EnvioCorreoVerificacionService envio = new EnvioCorreoVerificacionService(email, 5);

        envio.enviar("Ana", "ana@correo.com", "token");
        Thread.sleep(50);

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
        EmailVerificacionService email = (nombre, destinatario, token) -> {
            latch.countDown();
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("smtp caido");
            }
        };
        EnvioCorreoVerificacionService envio = new EnvioCorreoVerificacionService(email, 5);

        envio.enviar("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(50);
        assertThat(intentos.get()).isEqualTo(2);
    }
}
