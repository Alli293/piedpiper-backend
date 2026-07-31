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
class EmailInvitacionServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailInvitacionServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new EmailInvitacionServiceImpl(
                mailSender, "no-reply@carbonhub.com", "http://localhost:4200/registro/invitacion");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
    }

    @Test
    void enviarCorreoInvitacion_envuaUnCorreoHtmlConLaEmpresaYElEnlace() throws Exception {
        servicio.enviarCorreoInvitacion("ana.perez@example.com", "Consultora Verde CR", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Invitación a CarbonHub de Consultora Verde CR");
        assertThat(mensaje.getAllRecipients()[0]).hasToString("ana.perez@example.com");
        assertThat(mensaje.getFrom()[0]).hasToString("no-reply@carbonhub.com");
        assertThat(mensaje.getContentType()).contains("text/html");

        String html = (String) mensaje.getContent();
        assertThat(html)
                .contains("Consultora Verde CR")
                .contains("http://localhost:4200/registro/invitacion?token=token-123")
                .contains("Completar registro")
                .contains("#1f8a5b");
    }

    @Test
    void enviarCorreoInvitacion_escapaHtmlEnElNombreDeLaEmpresa() throws Exception {
        servicio.enviarCorreoInvitacion("ana.perez@example.com", "<script>alert(1)</script>", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = (String) captor.getValue().getContent();

        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void enviarCorreoInvitacion_siLaDireccionRemitenteEsInvalidaEnvuelveLaMessagingException() {
        EmailInvitacionServiceImpl servicioConRemitenteInvalido = new EmailInvitacionServiceImpl(
                mailSender, "a@b@c.com", "http://localhost:4200/registro/invitacion");

        assertThatThrownBy(() -> servicioConRemitenteInvalido.enviarCorreoInvitacion(
                "ana@example.com", "Consultora Verde CR", "token-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(jakarta.mail.internet.AddressException.class);
    }
}
