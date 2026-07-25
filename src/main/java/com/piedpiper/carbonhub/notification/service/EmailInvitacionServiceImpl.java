package com.piedpiper.carbonhub.notification.service;

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
public class EmailInvitacionServiceImpl implements EmailInvitacionService {

    private static final String EMAIL_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Invitacion a CarbonHub</title>
            </head>
            <body style="margin:0; padding:0; background-color:#f0f2f5; font-family:Arial, Helvetica, sans-serif;">
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f2f5; padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%%; background-color:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #e2e8f0;">
                      <tr>
                        <td style="padding:32px 40px 8px 40px;" align="left">
                          <span style="font-size:22px; font-weight:700; color:#0e2a3b;">Carbon</span><span style="font-size:22px; font-weight:700; color:#1f8a5b;">Hub</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:24px 40px 0 40px;">
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">Hola,</p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            <strong>%s</strong> te invita a unirte a su cuenta en CarbonHub. Para completar tu registro, haz clic en el siguiente botón:
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#1f8a5b;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Completar registro
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0 0 8px 0; font-size:13px; color:#64748b;">
                            Si no esperabas esta invitación, puedes ignorar este correo.
                          </p>
                          <p style="margin:0; font-size:13px; color:#64748b;">
                            Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                            <a href="%s" style="color:#2ba6de; word-break:break-all;">%s</a>
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:24px 40px; border-top:1px solid #e2e8f0;" align="center">
                          <p style="margin:0; font-size:12px; color:#8a9bae;">CarbonHub — Costa Rica</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

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
        return EMAIL_TEMPLATE.formatted(nombreEscapado, enlace, enlace, enlace);
    }
}
