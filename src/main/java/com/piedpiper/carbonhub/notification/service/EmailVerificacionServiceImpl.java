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
public class EmailVerificacionServiceImpl implements EmailVerificacionService {

    private static final String EMAIL_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verifica tu correo - CarbonHub</title>
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
                            Gracias por registrarte en CarbonHub. Para activar tu cuenta, verifica tu correo electrónico haciendo clic en el siguiente botón:
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#1f8a5b;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Verificar mi correo
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0 0 8px 0; font-size:13px; color:#64748b;">
                            Este enlace expira en 24 horas. Si no solicitaste esto, puedes ignorar este correo.
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
        return EMAIL_TEMPLATE.formatted(nombreEscapado, enlace, enlace, enlace);
    }
}
