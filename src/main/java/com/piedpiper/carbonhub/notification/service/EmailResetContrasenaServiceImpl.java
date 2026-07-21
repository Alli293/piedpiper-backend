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
public class EmailResetContrasenaServiceImpl implements EmailResetContrasenaService {

    private static final String PLANTILLA_RESET = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Restablece tu contraseña - CarbonHub</title>
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
                            Recibimos una solicitud para restablecer la contraseña de tu cuenta CarbonHub. Haz clic en el siguiente botón para elegir una nueva:
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#1f8a5b;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Restablecer contraseña
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0 0 8px 0; font-size:13px; color:#64748b;">
                            Este enlace expira en 1 hora. Si no solicitaste esto, puedes ignorar este correo — tu contraseña actual seguirá funcionando.
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

    private static final String PLANTILLA_USA_GOOGLE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Tu cuenta usa Google para iniciar sesión - CarbonHub</title>
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
                            Recibimos una solicitud para restablecer la contraseña de la cuenta CarbonHub asociada a este correo. Detectamos que tu cuenta usa Google para iniciar sesión — no tiene una contraseña que restablecer.
                          </p>
                          <p style="margin:0 0 24px 0; font-size:15px; line-height:1.6; color:#334155;">
                            Solo inicia sesión con el botón de Google en la pantalla de acceso.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 24px 40px;" align="center">
                          <table role="presentation" cellpadding="0" cellspacing="0">
                            <tr>
                              <td style="border-radius:8px; background-color:#2ba6de;">
                                <a href="%s" target="_blank" style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:8px;">
                                  Ir a iniciar sesión
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0; font-size:13px; color:#64748b;">
                            Si no solicitaste esto, puedes ignorar este correo.
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
    private final String resetContrasenaUrl;
    private final String loginUrl;

    public EmailResetContrasenaServiceImpl(JavaMailSender mailSender,
                                           @Value("${spring.mail.username}") String remitente,
                                           @Value("${frontend.reset-contrasena-url}") String resetContrasenaUrl,
                                           @Value("${frontend.login-url}") String loginUrl) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.resetContrasenaUrl = resetContrasenaUrl;
        this.loginUrl = loginUrl;
    }

    @Override
    public void enviarResetContrasena(String nombreDestinatario, String email, String token) {
        String enlace = resetContrasenaUrl + "?token=" + token;
        String nombreEscapado = HtmlUtils.htmlEscape(nombreDestinatario);
        String html = PLANTILLA_RESET.formatted(nombreEscapado, enlace, enlace, enlace);
        String textoPlano = "Hola %s,\n\n".formatted(nombreDestinatario)
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta CarbonHub. "
                + "Abre este enlace para elegir una nueva contraseña (expira en 1 hora):\n" + enlace + "\n\n"
                + "Si no solicitaste esto, puedes ignorar este correo — tu contraseña actual seguirá funcionando.";
        enviar(email, "Restablece tu contraseña - CarbonHub", textoPlano, html);
    }

    @Override
    public void enviarUsaGoogle(String nombreDestinatario, String email) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombreDestinatario);
        String html = PLANTILLA_USA_GOOGLE.formatted(nombreEscapado, loginUrl);
        String textoPlano = "Hola %s,\n\n".formatted(nombreDestinatario)
                + "Recibimos una solicitud para restablecer la contraseña de la cuenta CarbonHub asociada a "
                + "este correo. Tu cuenta usa Google para iniciar sesión, así que no tiene una contraseña que "
                + "restablecer. Inicia sesión aquí:\n" + loginUrl + "\n\n"
                + "Si no solicitaste esto, puedes ignorar este correo.";
        enviar(email, "Tu cuenta usa Google para iniciar sesión - CarbonHub", textoPlano, html);
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
            throw new IllegalStateException("No se pudo construir el correo de reset de contrasena", e);
        }
    }
}
