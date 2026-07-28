package com.piedpiper.carbonhub.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.piedpiper.carbonhub.notification.EmailPlantillaHtml;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class EmailInvitacionServiceImpl implements EmailInvitacionService {

    private static final String SALUDO = "Hola,";
    private static final String INTRO = "<strong>%s</strong> te invita a unirte a su cuenta en CarbonHub. "
            + "Para completar tu registro, haz clic en el siguiente botón:";
    private static final String AVISO = "Si no esperabas esta invitación, puedes ignorar este correo.";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String invitacionUrl;

    public EmailInvitacionServiceImpl(JavaMailSender mailSender,
                                      @Value("${spring.mail.username}") String remitente,
                                      @Value("${frontend.invitacion-url}") String invitacionUrl) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.invitacionUrl = invitacionUrl;
    }

    @Override
    public void enviarCorreoInvitacion(String email, String nombreEmpresa, String token) {
        String enlace = invitacionUrl + "?token=" + token;
        String html = construirHtml(nombreEmpresa, enlace);

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email);
            helper.setSubject("Invitación a CarbonHub de " + nombreEmpresa);
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo construir el correo de invitacion", e);
        }
    }

    private String construirHtml(String nombreEmpresa, String enlace) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombreEmpresa);
        String boton = EmailPlantillaHtml.boton(enlace, "Completar registro");
        String cuerpo = EmailPlantillaHtml.cuerpoConBotonYAviso(
                SALUDO, INTRO.formatted(nombreEscapado), boton, AVISO, enlace);
        return EmailPlantillaHtml.documento("Invitacion a CarbonHub", cuerpo);
    }
}
