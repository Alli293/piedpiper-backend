package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailAsignacionAuditorServiceStub implements EmailAsignacionAuditorService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAsignacionAuditorServiceStub.class);

    @Override
    public void enviarAsignacion(String nombreAuditor, String emailAuditor, String nombreEmpresa) {
        logger.info("Correo de asignacion (stub) para {} <{}>: te asignaron una auditoria de {}",
                nombreAuditor, emailAuditor, nombreEmpresa);
    }

    @Override
    public void enviarExpiracionAsignacion(String correoEmpresa, String nombreEmpresa, String nombreAuditor) {
        logger.info("Correo de expiracion de asignacion (stub) para {} <{}>: el auditor {} no respondio a tiempo",
                nombreEmpresa, correoEmpresa, nombreAuditor);
    }
}
