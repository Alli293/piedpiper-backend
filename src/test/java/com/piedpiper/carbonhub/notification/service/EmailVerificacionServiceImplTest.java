package com.piedpiper.carbonhub.notification.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificacionServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailVerificacionServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new EmailVerificacionServiceImpl(
                mailSender, "no-reply@carbonhub.com", "http://localhost:4200/verificar-correo");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
    }

    @Test
    void enviarCorreoVerificacion_envuaUnCorreoHtmlConElNombreYElEnlace() throws Exception {
        servicio.enviarCorreoVerificacion("Ana Perez", "ana.perez@example.com", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Verifica tu correo - CarbonHub");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("ana.perez@example.com");
        assertThat(mensaje.getFrom()[0].toString()).isEqualTo("no-reply@carbonhub.com");
        assertThat(mensaje.getContentType()).contains("text/html");

        String html = (String) mensaje.getContent();
        assertThat(html)
                .contains("Ana Perez")
                .contains("http://localhost:4200/verificar-correo?token=token-123")
                .contains("Verificar mi correo")
                .contains("#1f8a5b");
    }

    @Test
    void enviarCorreoVerificacion_escapaHtmlEnElNombreDelDestinatario() throws Exception {
        servicio.enviarCorreoVerificacion("<script>alert(1)</script>", "ana.perez@example.com", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = (String) captor.getValue().getContent();

        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void enviarCorreoVerificacion_siLaDireccionRemitenteEsInvalidaEnvuelveLaMessagingExceptionSinVerificada() {
        EmailVerificacionServiceImpl servicioConRemitenteInvalido = new EmailVerificacionServiceImpl(
                mailSender, "a@b@c.com", "http://localhost:4200/verificar-correo");

        assertThatThrownBy(() -> servicioConRemitenteInvalido.enviarCorreoVerificacion(
                "Ana", "ana@example.com", "token-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(jakarta.mail.internet.AddressException.class);
    }
}
