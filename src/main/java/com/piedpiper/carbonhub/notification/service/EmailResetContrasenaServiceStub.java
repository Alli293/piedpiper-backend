package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailResetContrasenaServiceStub implements EmailResetContrasenaService {

    private static final Logger logger = LoggerFactory.getLogger(EmailResetContrasenaServiceStub.class);

    private final String resetContrasenaUrl;
    private final String loginUrl;

    public EmailResetContrasenaServiceStub(
            @Value("${frontend.reset-contrasena-url}") String resetContrasenaUrl,
            @Value("${frontend.login-url}") String loginUrl) {
        this.resetContrasenaUrl = resetContrasenaUrl;
        this.loginUrl = loginUrl;
    }

    @Override
    public void enviarResetContrasena(String nombreDestinatario, String email, String token) {
        String enlace = resetContrasenaUrl + "?token=" + token;
        logger.info("Correo de reset de contrasena (stub) para {} <{}>: {}", nombreDestinatario, email, enlace);
    }

    @Override
    public void enviarUsaGoogle(String nombreDestinatario, String email) {
        logger.info("Correo de aviso de cuenta Google (stub) para {} <{}>: {}", nombreDestinatario, email, loginUrl);
    }
}
