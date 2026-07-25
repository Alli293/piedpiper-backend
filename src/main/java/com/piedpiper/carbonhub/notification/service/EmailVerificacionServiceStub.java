package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailVerificacionServiceStub implements EmailVerificacionService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificacionServiceStub.class);

    private final String verificarCorreoUrl;

    public EmailVerificacionServiceStub(
            @Value("${frontend.verificar-correo-url}") String verificarCorreoUrl) {
        this.verificarCorreoUrl = verificarCorreoUrl;
    }

    @Override
    public void enviarCorreoVerificacion(String nombreDestinatario, String email, String token) {
        String enlace = verificarCorreoUrl + "?token=" + token;
        logger.info("Correo de verificacion (stub) para {} <{}>: {}", nombreDestinatario, email, enlace);
    }
}
