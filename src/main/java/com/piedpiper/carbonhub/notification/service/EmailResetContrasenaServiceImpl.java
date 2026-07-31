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
public class EmailResetContrasenaServiceImpl implements EmailResetContrasenaService {

    private static final String SALUDO_RESET = "Hola %s,";
    private static final String INTRO_RESET = "Recibimos una solicitud para restablecer la contraseña de tu "
            + "cuenta CarbonHub. Haz clic en el siguiente botón para elegir una nueva:";
    private static final String AVISO_RESET = "Este enlace expira en 1 hora. Si no solicitaste esto, puedes "
            + "ignorar este correo — tu contraseña actual seguirá funcionando.";

    private static final String CUERPO_USA_GOOGLE = """
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
                      %s
                      <tr>
                        <td style="padding:0 40px 8px 40px;">
                          <p style="margin:0; font-size:13px; color:#64748b;">
                            Si no solicitaste esto, puedes ignorar este correo.
                          </p>
                        </td>
                      </tr>
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
        String boton = EmailPlantillaHtml.boton(enlace, "Restablecer contraseña");
        String cuerpo = EmailPlantillaHtml.cuerpoConBotonYAviso(
                SALUDO_RESET.formatted(nombreEscapado), INTRO_RESET, boton, AVISO_RESET, enlace);
        String html = EmailPlantillaHtml.documento("Restablece tu contraseña - CarbonHub", cuerpo);
        String textoPlano = "Hola %s,\n\n".formatted(nombreDestinatario)
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta CarbonHub. "
                + "Abre este enlace para elegir una nueva contraseña (expira en 1 hora):\n" + enlace + "\n\n"
                + "Si no solicitaste esto, puedes ignorar este correo — tu contraseña actual seguirá funcionando.";
        enviar(email, "Restablece tu contraseña - CarbonHub", textoPlano, html);
    }

    @Override
    public void enviarUsaGoogle(String nombreDestinatario, String email) {
        String nombreEscapado = HtmlUtils.htmlEscape(nombreDestinatario);
        String boton = EmailPlantillaHtml.boton(loginUrl, "Ir a iniciar sesión", "#2ba6de");
        String cuerpo = CUERPO_USA_GOOGLE.formatted(nombreEscapado, boton);
        String html = EmailPlantillaHtml.documento("Tu cuenta usa Google para iniciar sesión - CarbonHub", cuerpo);
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
