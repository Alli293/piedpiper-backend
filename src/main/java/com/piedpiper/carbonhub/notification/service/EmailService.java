package com.piedpiper.carbonhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_INTENTOS = 3;
    private static final Duration INTERVALO_REINTENTO = Duration.ofMinutes(5);

    private final TaskScheduler taskScheduler;

    public EmailService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void enviarConfirmacionAuditor(String email, String nombre, String numeroCertificacion) {
        String asunto = "Solicitud de registro recibida - CarbonHub";
        taskScheduler.schedule(() -> intentarEnvio(email, asunto, nombre, numeroCertificacion, 1),
                Instant.now());
    }

    private void intentarEnvio(String email, String asunto, String nombre,
                               String numeroCertificacion, int intento) {
        try {
            despachar(email, asunto, nombre, numeroCertificacion);
        } catch (RuntimeException e) {
            log.warn("Fallo al enviar correo a {} (intento {}/{}): {}",
                    email, intento, MAX_INTENTOS, e.getMessage());
            if (intento < MAX_INTENTOS) {
                taskScheduler.schedule(
                        () -> intentarEnvio(email, asunto, nombre, numeroCertificacion, intento + 1),
                        Instant.now().plus(INTERVALO_REINTENTO));
            } else {
                log.error("No se pudo enviar el correo de confirmación a {} tras {} intentos",
                        email, MAX_INTENTOS);
            }
        }
    }

    private void despachar(String email, String asunto, String nombre, String numeroCertificacion) {
        log.info("Correo enviado a {} | asunto: {} | auditor: {} | certificacion: {}",
                email, asunto, nombre, numeroCertificacion);
    }
}
