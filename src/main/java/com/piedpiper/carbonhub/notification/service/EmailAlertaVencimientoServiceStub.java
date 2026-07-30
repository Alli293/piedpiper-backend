package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailAlertaVencimientoServiceStub implements EmailAlertaVencimientoService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertaVencimientoServiceStub.class);

    @Override
    public void enviarAlertaVencimiento(String email,
                                        String nombreEmpresa,
                                        String nombreCertificacion,
                                        LocalDate fechaVencimiento,
                                        long diasRestantes,
                                        String urlCertificacion) {
        logger.info("Alerta de vencimiento (stub) para <{}>: {} de {} vence el {} ({} dias). Enlace: {}",
                email, nombreCertificacion, nombreEmpresa, fechaVencimiento, diasRestantes, urlCertificacion);
    }
}
