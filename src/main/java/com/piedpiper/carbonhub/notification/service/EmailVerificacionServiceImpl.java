package com.piedpiper.carbonhub.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class EmailVerificacionServiceImpl implements EmailVerificacionService {

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

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(email);
        mensaje.setSubject("Verifica tu correo - CarbonHub");
        mensaje.setText("Hola " + nombreDestinatario + ",\n\n"
                + "Verifica tu correo haciendo clic en el siguiente enlace:\n" + enlace + "\n\n"
                + "Este enlace expira en 24 horas.\n\n"
                + "Si no solicitaste esto, puedes ignorar este correo.");

        mailSender.send(mensaje);
    }
}
