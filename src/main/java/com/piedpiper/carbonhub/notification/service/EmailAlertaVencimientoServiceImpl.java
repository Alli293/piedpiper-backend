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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class EmailAlertaVencimientoServiceImpl implements EmailAlertaVencimientoService {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "CR"));

    private static final String SALUDO = "Hola,";
    private static final String INTRO = "La certificación <strong>%s</strong> de %s vence el "
            + "<strong>%s</strong>. %s";
    private static final String AVISO = "Si ya iniciaste el proceso de renovación, puedes ignorar este correo.";

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailAlertaVencimientoServiceImpl(JavaMailSender mailSender,
                                             @Value("${spring.mail.username}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviarAlertaVencimiento(String email,
                                        String nombreEmpresa,
                                        String nombreCertificacion,
                                        LocalDate fechaVencimiento,
                                        long diasRestantes,
                                        String urlCertificacion) {
        String html = construirHtml(nombreEmpresa, nombreCertificacion, fechaVencimiento,
                diasRestantes, urlCertificacion);

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(email);
            helper.setSubject(asunto(nombreCertificacion, diasRestantes));
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo construir el correo de alerta de vencimiento", e);
        }
    }

    private static String asunto(String nombreCertificacion, long diasRestantes) {
        return "%s: %s".formatted(textoDias(diasRestantes), nombreCertificacion);
    }

    /**
     * Los dias restantes se calculan contra la fecha real de vencimiento y no contra el umbral, para
     * que el correo no diga "vence en 90 dias" cuando el proceso se atraso y en realidad quedan 85.
     */
    private static String textoDias(long diasRestantes) {
        if (diasRestantes < 0) {
            return "Certificación vencida";
        }
        if (diasRestantes == 0) {
            return "Tu certificación vence hoy";
        }
        return diasRestantes == 1 ? "Tu certificación vence mañana"
                : "Tu certificación vence en %d días".formatted(diasRestantes);
    }

    private static String frasePlazo(long diasRestantes) {
        if (diasRestantes < 0) {
            return "Ya venció, por lo que conviene iniciar la renovación cuanto antes.";
        }
        if (diasRestantes == 0) {
            return "Vence hoy.";
        }
        return diasRestantes == 1 ? "Queda 1 día."
                : "Quedan %d días.".formatted(diasRestantes);
    }

    private String construirHtml(String nombreEmpresa,
                                 String nombreCertificacion,
                                 LocalDate fechaVencimiento,
                                 long diasRestantes,
                                 String urlCertificacion) {
        String intro = INTRO.formatted(
                HtmlUtils.htmlEscape(nombreCertificacion),
                HtmlUtils.htmlEscape(nombreEmpresa),
                FORMATO_FECHA.format(fechaVencimiento),
                frasePlazo(diasRestantes));
        String boton = EmailPlantillaHtml.boton(urlCertificacion, "Ver certificación");
        String cuerpo = EmailPlantillaHtml.cuerpoConBotonYAviso(
                SALUDO, intro, boton, AVISO, urlCertificacion);
        return EmailPlantillaHtml.documento("Vencimiento de certificacion", cuerpo);
    }
}
