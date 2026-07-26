package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.notification.service.EmailInvitacionService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnvioCorreoInvitacionServiceTest {

    @Test
    void envioExitosoNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        EmailInvitacionService email = (destinatario, empresa, token) -> {
            intentos.incrementAndGet();
            latch.countDown();
        };
        EnvioCorreoInvitacionService envio = new EnvioCorreoInvitacionService(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void envioFallidoReintentaHastaTresVeces() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        EmailInvitacionService email = (destinatario, empresa, token) -> {
            latch.countDown();
            throw new IllegalStateException("smtp caido");
        };
        EnvioCorreoInvitacionService envio = new EnvioCorreoInvitacionService(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void reintentoExitosoDetieneLosSiguientes() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        EmailInvitacionService email = (destinatario, empresa, token) -> {
            boolean primerIntento = intentos.incrementAndGet() == 1;
            latch.countDown();
            if (primerIntento) {
                throw new IllegalStateException("smtp caido");
            }
        };
        EnvioCorreoInvitacionService envio = new EnvioCorreoInvitacionService(email, 5);

        envio.enviar("colab@correo.com", "Acme S.A.", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(intentos.get()).isEqualTo(2);
    }
}
