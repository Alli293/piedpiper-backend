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
public class EmailValidacionAuditorServiceImpl implements EmailValidacionAuditorService {

    private static final String CUERPO_APROBADO = """
                      <tr>
                        <td style="padding:24px 40px 0 40px;">
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">Hola %s,</p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            Tu solicitud para convertirte en auditor en CarbonHub fue <strong>aprobada</strong>. Ya puedes iniciar sesión y acceder a las funciones de auditoría.
                          </p>
                        </td>
                      </tr>
                      %s
            """;

    private static final String CUERPO_RECHAZADO = """
                      <tr>
                        <td style="padding:24px 40px 0 40px;">
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">Hola %s,</p>
                          <p style="margin:0 0 8px 0; font-size:15px; line-height:1.6; color:#334155;">
                            Tu solicitud para convertirte en auditor en CarbonHub fue <strong>rechazada</strong>.
                          </p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            Motivo: %s
                          </p>
                        </td>
                      </tr>
            """;

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String loginUrl;

    public EmailValidacionAuditorServiceImpl(JavaMailSender mailSender,
                                             @Value("${spring.mail.username}") String remitente,
                                             @Value("${frontend.login-url}") String loginUrl) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.loginUrl = loginUrl;
    }

    @Override
    public void enviarResultadoValidacion(String nombre, String email, boolean aprobado, String motivoRechazo) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombre);
        if (aprobado) {
            String boton = EmailPlantillaHtml.boton(loginUrl, "Ir a iniciar sesión");
            String cuerpo = CUERPO_APROBADO.formatted(nombreEscapado, boton);
            String html = EmailPlantillaHtml.documento("Solicitud de auditor aprobada - CarbonHub", cuerpo);
            String textoPlano = "Hola %s,\n\n".formatted(nombre)
                    + "Tu solicitud para convertirte en auditor en CarbonHub fue aprobada. "
                    + "Ya puedes iniciar sesión aquí:\n" + loginUrl;
            enviar(email, "Solicitud de auditor aprobada - CarbonHub", textoPlano, html);
        } else {
            String motivoEscapado = HtmlUtils.htmlEscape(motivoRechazo);
            String cuerpo = CUERPO_RECHAZADO.formatted(nombreEscapado, motivoEscapado);
            String html = EmailPlantillaHtml.documento("Solicitud de auditor rechazada - CarbonHub", cuerpo);
            String textoPlano = "Hola %s,\n\n".formatted(nombre)
                    + "Tu solicitud para convertirte en auditor en CarbonHub fue rechazada.\n\n"
                    + "Motivo: " + motivoRechazo;
            enviar(email, "Solicitud de auditor rechazada - CarbonHub", textoPlano, html);
        }
    }

    private void enviar(String destinatario, String asunto, String textoPlano, String html) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(textoPlano, html);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo construir el correo de validacion de auditor", e);
        }
    }
}
