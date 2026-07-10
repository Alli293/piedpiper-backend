package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificacionServiceStub implements EmailVerificacionService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificacionServiceStub.class);

    private final String verificarCorreoUrl;

    public EmailVerificacionServiceStub(
            @Value("${frontend.verificar-correo-url}") String verificarCorreoUrl) {
        this.verificarCorreoUrl = verificarCorreoUrl;
    }

    // TODO: reemplazar cuando PP-33 este disponible (envio real por SMTP/proveedor externo).
    @Override
    public void enviarCorreoVerificacion(String nombreDestinatario, String email, String token) {
        String enlace = verificarCorreoUrl + "?token=" + token;
        logger.info("Correo de verificacion (stub) para {} <{}>: {}", nombreDestinatario, email, enlace);
    }
}
