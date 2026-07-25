package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailValidacionAuditorServiceStub implements EmailValidacionAuditorService {

    private static final Logger logger = LoggerFactory.getLogger(EmailValidacionAuditorServiceStub.class);

    @Override
    public void enviarResultadoValidacion(String nombre, String email, boolean aprobado, String motivoRechazo) {
        if (aprobado) {
            logger.info("Correo de validacion (stub) para {} <{}>: solicitud aprobada", nombre, email);
        } else {
            logger.info("Correo de validacion (stub) para {} <{}>: solicitud rechazada, motivo: {}",
                    nombre, email, motivoRechazo);
        }
    }
}
