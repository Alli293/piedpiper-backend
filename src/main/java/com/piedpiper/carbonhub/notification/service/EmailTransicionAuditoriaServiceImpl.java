package com.piedpiper.carbonhub.notification.service;

import com.piedpiper.carbonhub.notification.EmailPlantillaHtml;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class EmailTransicionAuditoriaServiceImpl implements EmailTransicionAuditoriaService {

    private static final String ASUNTO = "Tu auditoría cambió de estado - CarbonHub";

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailTransicionAuditoriaServiceImpl(JavaMailSender mailSender,
                                               @Value("${spring.mail.username}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    /**
     * No captura excepciones a proposito: el barrido que invoca este envio necesita enterarse del
     * fallo para contabilizar el intento y reintentar. Tragarlo aca dejaria la notificacion marcada
     * como enviada sin haberlo sido.
     */
    @Override
    public void enviarCambioEstado(String destinatario,
                                   String nombreDestinatario,
                                   String nombreEmpresa,
                                   String estadoLegible,
                                   String urlDetalle) {
        String intro = """
                La solicitud de auditoría de <strong>%s</strong> pasó al estado
                <strong>%s</strong>. Ingresa a la plataforma para ver el detalle y la línea de
                tiempo completa del proceso.
                """.formatted(HtmlUtils.htmlEscape(nombreEmpresa), HtmlUtils.htmlEscape(estadoLegible));

        String cuerpo = EmailPlantillaHtml.cuerpoConBotonYAviso(
                "Hola %s,".formatted(HtmlUtils.htmlEscape(nombreDestinatario)),
                intro,
                EmailPlantillaHtml.boton(urlDetalle, "Ver la auditoría"),
                "Si el botón no funciona, copia y pega esta dirección en tu navegador:",
                urlDetalle);

        enviar(destinatario, EmailPlantillaHtml.documento(ASUNTO, cuerpo));
    }

    private void enviar(String destinatario, String html) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(ASUNTO);
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo construir el correo de cambio de estado", e);
        }
    }
}
