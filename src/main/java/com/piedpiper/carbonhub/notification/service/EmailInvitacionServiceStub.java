package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailInvitacionServiceStub implements EmailInvitacionService {

    private static final Logger logger = LoggerFactory.getLogger(EmailInvitacionServiceStub.class);

    private final String invitacionUrl;

    public EmailInvitacionServiceStub(
            @Value("${frontend.invitacion-url}") String invitacionUrl) {
        this.invitacionUrl = invitacionUrl;
    }

    @Override
    public void enviarCorreoInvitacion(String email, String nombreEmpresa, String token) {
        String enlace = invitacionUrl + "?token=" + token;
        logger.info("Correo de invitacion (stub) de {} para <{}>: {}", nombreEmpresa, email, enlace);
    }
}
