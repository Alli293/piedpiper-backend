package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificacionServiceStub implements EmailVerificacionService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificacionServiceStub.class);

    // TODO: reemplazar cuando PP-33 y la entidad Usuario real esten disponibles en esta rama.
    @Override
    public void enviarCorreoVerificacion(String nombreDestinatario, String email) {
        logger.info("Correo de verificacion (stub) para {} <{}>", nombreDestinatario, email);
    }
}
