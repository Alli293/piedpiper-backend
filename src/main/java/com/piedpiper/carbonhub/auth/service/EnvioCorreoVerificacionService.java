package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EnvioCorreoVerificacionService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoVerificacionService.class);
    private static final int MAX_REINTENTOS = 3;

    private final EmailVerificacionService emailVerificacionService;
    private final long intervaloReintentoMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EnvioCorreoVerificacionService(EmailVerificacionService emailVerificacionService,
                                 @Value("${verificacion.reintento-intervalo-ms:300000}") long intervaloReintentoMs) {
        this.emailVerificacionService = emailVerificacionService;
        this.intervaloReintentoMs = intervaloReintentoMs;
    }

    public void enviar(String nombreDestinatario, String email, String token) {
        intentar(nombreDestinatario, email, token, MAX_REINTENTOS);
    }

    private void intentar(String nombreDestinatario, String email, String token, int reintentosRestantes) {
        try {
            emailVerificacionService.enviarCorreoVerificacion(nombreDestinatario, email, token);
        } catch (Exception e) {
            log.error("Fallo el envio del correo de reenvio de verificacion a {} (reintentos restantes: {})",
                    email, reintentosRestantes, e);
            if (reintentosRestantes > 0) {
                scheduler.schedule(
                        () -> intentar(nombreDestinatario, email, token, reintentosRestantes - 1),
                        intervaloReintentoMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    @PreDestroy
    void cerrar() {
        scheduler.shutdown();
    }
}
