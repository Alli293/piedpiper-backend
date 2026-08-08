package com.piedpiper.carbonhub.notification.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EmailResetContrasenaServiceStubTest {

    private final EmailResetContrasenaServiceStub servicio = new EmailResetContrasenaServiceStub(
            "http://localhost:4200/reset-contrasena", "http://localhost:4200/login");
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void prepararLogger() {
        logger = (Logger) LoggerFactory.getLogger(EmailResetContrasenaServiceStub.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void limpiarLogger() {
        logger.detachAppender(logAppender);
    }

    @Test
    void enviarResetContrasena_noLanzaExcepcion() {
        assertThatCode(() -> servicio.enviarResetContrasena(
                "Ana Perez", "ana.perez@example.com", "token-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarResetContrasena_noExponeElTokenEnLogs() {
        servicio.enviarResetContrasena("Ana Perez", "ana.perez@example.com", "token-123");

        assertThat(logAppender.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel()).isEqualTo(Level.INFO);
                    assertThat(evento.getFormattedMessage())
                            .contains("Ana Perez")
                            .contains("ana.perez@example.com")
                            .doesNotContain("token-123")
                            .doesNotContain("?token=");
                });
    }

    @Test
    void enviarUsaGoogle_registraElAvisoEnElLogConLaUrlDeLogin() {
        servicio.enviarUsaGoogle("Ana Perez", "ana.perez@example.com");

        assertThat(logAppender.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel()).isEqualTo(Level.INFO);
                    assertThat(evento.getFormattedMessage())
                            .contains("Ana Perez")
                            .contains("ana.perez@example.com")
                            .contains("http://localhost:4200/login");
                });
    }
}
