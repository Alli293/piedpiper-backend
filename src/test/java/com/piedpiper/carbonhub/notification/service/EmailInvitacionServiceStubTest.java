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

class EmailInvitacionServiceStubTest {

    private final EmailInvitacionServiceStub servicio =
            new EmailInvitacionServiceStub("http://localhost:4200/registro/invitacion");
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void prepararLogger() {
        logger = (Logger) LoggerFactory.getLogger(EmailInvitacionServiceStub.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void limpiarLogger() {
        logger.detachAppender(logAppender);
    }

    @Test
    void enviarCorreoInvitacion_noLanzaExcepcion() {
        assertThatCode(() -> servicio.enviarCorreoInvitacion(
                "ana.perez@example.com", "Consultora Verde CR", "token-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarCorreoInvitacion_noExponeElTokenEnLogs() {
        servicio.enviarCorreoInvitacion("ana.perez@example.com", "Consultora Verde CR", "token-123");

        assertThat(logAppender.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel()).isEqualTo(Level.INFO);
                    assertThat(evento.getFormattedMessage())
                            .contains("Consultora Verde CR")
                            .contains("ana.perez@example.com")
                            .doesNotContain("token-123")
                            .doesNotContain("?token=");
                });
    }
}
