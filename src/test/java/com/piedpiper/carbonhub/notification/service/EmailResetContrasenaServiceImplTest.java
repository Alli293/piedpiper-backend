package com.piedpiper.carbonhub.notification.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
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
class EmailResetContrasenaServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailResetContrasenaServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new EmailResetContrasenaServiceImpl(
                mailSender, "no-reply@carbonhub.com",
                "http://localhost:4200/reset-contrasena", "http://localhost:4200/login");
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
    }

    private static String extraerParte(Object contenido, String mimeType) throws Exception {
        if (contenido instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart parte = multipart.getBodyPart(i);
                if (parte.isMimeType(mimeType)) {
                    return (String) parte.getContent();
                }
                String anidado = extraerParte(parte.getContent(), mimeType);
                if (anidado != null) {
                    return anidado;
                }
            }
        }
        return null;
    }

    @Test
    void enviarResetContrasena_envuaUnCorreoHtmlConElNombreYElEnlace() throws Exception {
        servicio.enviarResetContrasena("Ana Perez", "ana.perez@example.com", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Restablece tu contraseña - CarbonHub");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("ana.perez@example.com");
        assertThat(mensaje.getFrom()[0].toString()).isEqualTo("no-reply@carbonhub.com");
        assertThat(mensaje.getContentType()).containsIgnoringCase("multipart");

        String html = extraerParte(mensaje.getContent(), "text/html");
        assertThat(html)
                .contains("Ana Perez")
                .contains("http://localhost:4200/reset-contrasena?token=token-123")
                .contains("Restablecer contraseña")
                .contains("#1f8a5b");

        String textoPlano = extraerParte(mensaje.getContent(), "text/plain");
        assertThat(textoPlano)
                .contains("Ana Perez")
                .contains("http://localhost:4200/reset-contrasena?token=token-123");
    }

    @Test
    void enviarResetContrasena_escapaHtmlEnElNombreDelDestinatario() throws Exception {
        servicio.enviarResetContrasena("<script>alert(1)</script>", "ana.perez@example.com", "token-123");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();
        String html = extraerParte(mensaje.getContent(), "text/html");

        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void enviarUsaGoogle_envuaUnCorreoHtmlSinTokenConEnlaceALogin() throws Exception {
        servicio.enviarUsaGoogle("Ana Perez", "ana.perez@example.com");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Tu cuenta usa Google para iniciar sesión - CarbonHub");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("ana.perez@example.com");
        assertThat(mensaje.getContentType()).containsIgnoringCase("multipart");

        String html = extraerParte(mensaje.getContent(), "text/html");
        assertThat(html)
                .contains("Ana Perez")
                .contains("usa Google")
                .contains("http://localhost:4200/login")
                .contains("Ir a iniciar sesión")
                .doesNotContain("Restablecer contraseña");

        String textoPlano = extraerParte(mensaje.getContent(), "text/plain");
        assertThat(textoPlano)
                .contains("Ana Perez")
                .contains("usa Google")
                .contains("http://localhost:4200/login");
    }

    @Test
    void enviarResetContrasena_siLaDireccionRemitenteEsInvalidaEnvuelveLaMessagingExceptionSinVerificada() {
        EmailResetContrasenaServiceImpl servicioConRemitenteInvalido = new EmailResetContrasenaServiceImpl(
                mailSender, "a@b@c.com",
                "http://localhost:4200/reset-contrasena", "http://localhost:4200/login");

        assertThatThrownBy(() -> servicioConRemitenteInvalido.enviarResetContrasena(
                "Ana", "ana@example.com", "token-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(jakarta.mail.internet.AddressException.class);
    }
}
