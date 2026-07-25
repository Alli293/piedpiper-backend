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
public class EmailVerificacionServiceImpl implements EmailVerificacionService {

    private static final String SALUDO = "Hola %s,";
    private static final String INTRO = "Gracias por registrarte en CarbonHub. Para activar tu cuenta, "
            + "verifica tu correo electrónico haciendo clic en el siguiente botón:";
    private static final String AVISO = "Este enlace expira en 24 horas. Si no solicitaste esto, "
            + "puedes ignorar este correo.";

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String verificarCorreoUrl;

    public EmailVerificacionServiceImpl(JavaMailSender mailSender,
                                        @Value("${spring.mail.username}") String remitente,
                                        @Value("${frontend.verificar-correo-url}") String verificarCorreoUrl) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.verificarCorreoUrl = verificarCorreoUrl;
    }

    @Override
    public void enviarCorreoVerificacion(String nombreDestinatario, String email, String token) {
        String enlace = verificarCorreoUrl + "?token=" + token;
        String html = construirHtml(nombreDestinatario, enlace);

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email);
            helper.setSubject("Verifica tu correo - CarbonHub");
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo construir el correo de verificacion", e);
        }
    }

    private String construirHtml(String nombreDestinatario, String enlace) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombreDestinatario);
        String boton = EmailPlantillaHtml.boton(enlace, "Verificar mi correo");
        String cuerpo = EmailPlantillaHtml.cuerpoConBotonYAviso(
                SALUDO.formatted(nombreEscapado), INTRO, boton, AVISO, enlace);
        return EmailPlantillaHtml.documento("Verifica tu correo - CarbonHub", cuerpo);
    }
}
