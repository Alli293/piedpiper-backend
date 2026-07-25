package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.notification.service.EmailResetContrasenaService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EnvioCorreoResetContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoResetContrasenaService.class);
    private static final int MAX_REINTENTOS = 3;

    private final EmailResetContrasenaService emailResetContrasenaService;
    private final long intervaloReintentoMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EnvioCorreoResetContrasenaService(EmailResetContrasenaService emailResetContrasenaService,
                                 @Value("${reset-contrasena.reintento-intervalo-ms:300000}") long intervaloReintentoMs) {
        this.emailResetContrasenaService = emailResetContrasenaService;
        this.intervaloReintentoMs = intervaloReintentoMs;
    }

    public void enviarReset(String nombreDestinatario, String email, String token) {
        intentar(() -> emailResetContrasenaService.enviarResetContrasena(nombreDestinatario, email, token),
                "reset de contrasena", email, MAX_REINTENTOS);
    }

    public void enviarUsaGoogle(String nombreDestinatario, String email) {
        intentar(() -> emailResetContrasenaService.enviarUsaGoogle(nombreDestinatario, email),
                "aviso de cuenta Google", email, MAX_REINTENTOS);
    }

    private void intentar(Runnable envio, String descripcion, String email, int reintentosRestantes) {
        try {
            envio.run();
        } catch (Exception e) {
            log.error("Fallo el envio del correo de {} a {} (reintentos restantes: {})",
                    descripcion, email, reintentosRestantes, e);
            if (reintentosRestantes > 0) {
                scheduler.schedule(
                        () -> intentar(envio, descripcion, email, reintentosRestantes - 1),
                        intervaloReintentoMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    @PreDestroy
    void cerrar() {
        scheduler.shutdown();
    }
}
