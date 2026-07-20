package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.notification.service.EmailResetContrasenaService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnvioCorreoResetContrasenaServiceTest {

    @Test
    void enviarResetExitosoNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        EmailResetContrasenaService email = new EmailResetContrasenaService() {
            @Override
            public void enviarResetContrasena(String nombre, String destinatario, String token) {
                intentos.incrementAndGet();
            }

            @Override
            public void enviarUsaGoogle(String nombre, String destinatario) {
                throw new UnsupportedOperationException("no usado en este test");
            }
        };
        EnvioCorreoResetContrasenaService envio = new EnvioCorreoResetContrasenaService(email, 5);

        envio.enviarReset("Ana", "ana@correo.com", "token");
        Thread.sleep(50);

        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void enviarResetFallidoReintentaHastaTresVeces() throws Exception {
        CountDownLatch latch = new CountDownLatch(4);
        EmailResetContrasenaService email = new EmailResetContrasenaService() {
            @Override
            public void enviarResetContrasena(String nombre, String destinatario, String token) {
                latch.countDown();
                throw new IllegalStateException("smtp caido");
            }

            @Override
            public void enviarUsaGoogle(String nombre, String destinatario) {
                throw new UnsupportedOperationException("no usado en este test");
            }
        };
        EnvioCorreoResetContrasenaService envio = new EnvioCorreoResetContrasenaService(email, 5);

        envio.enviarReset("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void enviarUsaGoogleExitosoNoReintenta() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        EmailResetContrasenaService email = new EmailResetContrasenaService() {
            @Override
            public void enviarResetContrasena(String nombre, String destinatario, String token) {
                throw new UnsupportedOperationException("no usado en este test");
            }

            @Override
            public void enviarUsaGoogle(String nombre, String destinatario) {
                intentos.incrementAndGet();
            }
        };
        EnvioCorreoResetContrasenaService envio = new EnvioCorreoResetContrasenaService(email, 5);

        envio.enviarUsaGoogle("Ana", "ana@correo.com");
        Thread.sleep(50);

        assertThat(intentos.get()).isEqualTo(1);
    }

    @Test
    void reintentoExitosoDetieneLosSiguientes() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        EmailResetContrasenaService email = new EmailResetContrasenaService() {
            @Override
            public void enviarResetContrasena(String nombre, String destinatario, String token) {
                latch.countDown();
                if (intentos.incrementAndGet() == 1) {
                    throw new IllegalStateException("smtp caido");
                }
            }

            @Override
            public void enviarUsaGoogle(String nombre, String destinatario) {
                throw new UnsupportedOperationException("no usado en este test");
            }
        };
        EnvioCorreoResetContrasenaService envio = new EnvioCorreoResetContrasenaService(email, 5);

        envio.enviarReset("Ana", "ana@correo.com", "token");

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(50);
        assertThat(intentos.get()).isEqualTo(2);
    }
}
