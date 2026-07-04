package com.piedpiper.carbonhub.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_INTENTOS = 3;

    @Async
    public void enviarConfirmacionAuditor(String email, String nombre, String numeroCertificacion) {
        String asunto = "Solicitud de registro recibida - CarbonHub";
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                despachar(email, asunto, nombre, numeroCertificacion);
                return;
            } catch (RuntimeException e) {
                log.warn("Fallo al enviar correo a {} (intento {}/{}): {}",
                        email, intento, MAX_INTENTOS, e.getMessage());
            }
        }
        log.error("No se pudo enviar el correo de confirmación a {} tras {} intentos",
                email, MAX_INTENTOS);
    }

    private void despachar(String email, String asunto, String nombre, String numeroCertificacion) {
        log.info("Correo enviado a {} | asunto: {} | auditor: {} | certificacion: {}",
                email, asunto, nombre, numeroCertificacion);
    }
}
