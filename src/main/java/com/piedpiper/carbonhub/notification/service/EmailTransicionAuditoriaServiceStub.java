package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "stub", matchIfMissing = true)
public class EmailTransicionAuditoriaServiceStub implements EmailTransicionAuditoriaService {

    private static final Logger logger = LoggerFactory.getLogger(EmailTransicionAuditoriaServiceStub.class);

    @Override
    public void enviarCambioEstado(String destinatario,
                                   String nombreDestinatario,
                                   String nombreEmpresa,
                                   String estadoLegible,
                                   String urlDetalle) {
        logger.info("Correo de cambio de estado (stub) para {} <{}>: la auditoria de {} paso a {} ({})",
                nombreDestinatario, destinatario, nombreEmpresa, estadoLegible, urlDetalle);
    }
}
