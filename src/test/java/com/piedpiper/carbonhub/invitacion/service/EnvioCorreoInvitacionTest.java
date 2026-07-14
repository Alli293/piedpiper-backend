package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.notification.service.EmailInvitacionService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnvioCorreoInvitacionTest {

    @Test
    void envioExitosoNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        EmailInvitacionService email = (destinatario, empresa, token) -> intentos.incrementAndGet();
        EnvioCorreoInvitacion envio = new EnvioCorreoInvitacion(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");
        Thread.sleep(50);

        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void envioFallidoReintentaHastaTresVeces() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        EmailInvitacionService email = (destinatario, empresa, token) -> {
            latch.countDown();
            throw new IllegalStateException("smtp caido");
        };
        EnvioCorreoInvitacion envio = new EnvioCorreoInvitacion(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void reintentoExitosoDetieneLosSiguientes() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        EmailInvitacionService email = (destinatario, empresa, token) -> {
            latch.countDown();
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("smtp caido");
            }
        };
        EnvioCorreoInvitacion envio = new EnvioCorreoInvitacion(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(50);
        assertThat(intentos.get()).isEqualTo(2);
    }
}
