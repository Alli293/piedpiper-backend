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
class EmailValidacionAuditorServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailValidacionAuditorServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new EmailValidacionAuditorServiceImpl(
                mailSender, "no-reply@carbonhub.com", "http://localhost:4200/login");
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
    void enviarResultadoValidacion_aprobadoEnvuaUnCorreoHtmlConEnlaceALogin() throws Exception {
        servicio.enviarResultadoValidacion("Ana Perez", "ana.perez@example.com", true, null);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Solicitud de auditor aprobada - CarbonHub");
        assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("ana.perez@example.com");
        assertThat(mensaje.getFrom()[0].toString()).isEqualTo("no-reply@carbonhub.com");

        String html = extraerParte(mensaje.getContent(), "text/html");
        assertThat(html)
                .contains("Ana Perez")
                .contains("aprobada")
                .contains("http://localhost:4200/login")
                .contains("Ir a iniciar sesión");

        String textoPlano = extraerParte(mensaje.getContent(), "text/plain");
        assertThat(textoPlano)
                .contains("Ana Perez")
                .contains("aprobada")
                .contains("http://localhost:4200/login");
    }

    @Test
    void enviarResultadoValidacion_rechazadoEnvuaUnCorreoHtmlConElMotivo() throws Exception {
        servicio.enviarResultadoValidacion("Ana Perez", "ana.perez@example.com", false, "Documentacion incompleta");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();

        assertThat(mensaje.getSubject()).isEqualTo("Solicitud de auditor rechazada - CarbonHub");

        String html = extraerParte(mensaje.getContent(), "text/html");
        assertThat(html)
                .contains("Ana Perez")
                .contains("rechazada")
                .contains("Documentacion incompleta");

        String textoPlano = extraerParte(mensaje.getContent(), "text/plain");
        assertThat(textoPlano)
                .contains("Ana Perez")
                .contains("rechazada")
                .contains("Documentacion incompleta");
    }

    @Test
    void enviarResultadoValidacion_escapaHtmlEnElNombreYElMotivo() throws Exception {
        servicio.enviarResultadoValidacion(
                "<script>alert(1)</script>", "ana.perez@example.com", false, "<b>motivo</b>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        mensaje.saveChanges();
        String html = extraerParte(mensaje.getContent(), "text/html");

        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .doesNotContain("<b>motivo</b>")
                .contains("&lt;script&gt;")
                .contains("&lt;b&gt;motivo&lt;/b&gt;");
    }

    @Test
    void enviarResultadoValidacion_siLaDireccionRemitenteEsInvalidaEnvuelveLaMessagingException() {
        EmailValidacionAuditorServiceImpl servicioConRemitenteInvalido = new EmailValidacionAuditorServiceImpl(
                mailSender, "a@b@c.com", "http://localhost:4200/login");

        assertThatThrownBy(() -> servicioConRemitenteInvalido.enviarResultadoValidacion(
                "Ana", "ana@example.com", true, null))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(jakarta.mail.internet.AddressException.class);
    }
}
