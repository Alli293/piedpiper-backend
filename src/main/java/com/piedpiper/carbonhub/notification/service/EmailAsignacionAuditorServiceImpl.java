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
public class EmailAsignacionAuditorServiceImpl implements EmailAsignacionAuditorService {

    private static final String PLANTILLA_ASIGNACION = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Nueva auditoría asignada - CarbonHub</title>
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
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">Hola %s,</p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            <strong>%s</strong> te asignó una solicitud de auditoría en CarbonHub. Ingresa a la plataforma para revisar los detalles y responder.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#1f8a5b;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Ver la solicitud
                                </a>
                              </td>
                            </tr>
                          </table>
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

    private static final String PLANTILLA_EXPIRACION = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Asignación de auditor expirada - CarbonHub</title>
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
                          <p style="margin:0 0 16px 0; font-size:16px; color:#0e2a3b;">Hola %s,</p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            El auditor <strong>%s</strong> no respondió a tu solicitud de auditoría dentro del plazo establecido, por lo que la asignación fue liberada. Puedes elegir otro auditor desde el directorio.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#1f8a5b;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Asignar otro auditor
                                </a>
                              </td>
                            </tr>
                          </table>
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
    private final String loginUrl;

    public EmailAsignacionAuditorServiceImpl(JavaMailSender mailSender,
                                             @Value("${spring.mail.username}") String remitente,
                                             @Value("${frontend.login-url}") String loginUrl) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.loginUrl = loginUrl;
    }

    @Override
    public void enviarAsignacion(String nombreAuditor, String emailAuditor, String nombreEmpresa) {
        String html = PLANTILLA_ASIGNACION.formatted(
                HtmlUtils.htmlEscape(nombreAuditor), HtmlUtils.htmlEscape(nombreEmpresa), loginUrl);
        String textoPlano = "Hola %s,\n\n".formatted(nombreAuditor)
                + "%s te asignó una solicitud de auditoría en CarbonHub. ".formatted(nombreEmpresa)
                + "Ingresa a la plataforma para revisarla:\n" + loginUrl;
        enviar(emailAuditor, "Nueva auditoría asignada - CarbonHub", textoPlano, html);
    }

    @Override
    public void enviarExpiracionAsignacion(String correoEmpresa, String nombreEmpresa, String nombreAuditor) {
        String html = PLANTILLA_EXPIRACION.formatted(
                HtmlUtils.htmlEscape(nombreEmpresa), HtmlUtils.htmlEscape(nombreAuditor), loginUrl);
        String textoPlano = "Hola %s,\n\n".formatted(nombreEmpresa)
                + "El auditor %s no respondió a tu solicitud de auditoría dentro del plazo establecido, "
                        .formatted(nombreAuditor)
                + "por lo que la asignación fue liberada. Puedes elegir otro auditor aquí:\n" + loginUrl;
        enviar(correoEmpresa, "Asignación de auditor expirada - CarbonHub", textoPlano, html);
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
            throw new IllegalStateException("No se pudo construir el correo de asignacion de auditor", e);
        }
    }
}
