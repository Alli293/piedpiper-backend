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

class EmailVerificacionServiceStubTest {

    private final EmailVerificacionServiceStub servicio =
            new EmailVerificacionServiceStub("http://localhost:4200/verificar-correo");
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void prepararLogger() {
        logger = (Logger) LoggerFactory.getLogger(EmailVerificacionServiceStub.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void limpiarLogger() {
        logger.detachAppender(logAppender);
    }

    @Test
    void enviarCorreoVerificacion_noLanzaExcepcion() {
        assertThatCode(() -> servicio.enviarCorreoVerificacion(
                "Ana Perez", "ana.perez@example.com", "token-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarCorreoVerificacion_registraElEnvioEnElLogConLaUrlCompleta() {
        servicio.enviarCorreoVerificacion("Ana Perez", "ana.perez@example.com", "token-123");

        assertThat(logAppender.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel()).isEqualTo(Level.INFO);
                    assertThat(evento.getFormattedMessage())
                            .contains("Ana Perez")
                            .contains("ana.perez@example.com")
                            .contains("http://localhost:4200/verificar-correo?token=token-123");
                });
    }
}
