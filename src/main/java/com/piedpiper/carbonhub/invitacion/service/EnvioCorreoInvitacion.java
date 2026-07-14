package com.piedpiper.carbonhub.invitacion.service;

import com.piedpiper.carbonhub.notification.service.EmailInvitacionService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EnvioCorreoInvitacion {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoInvitacion.class);
    private static final int MAX_REINTENTOS = 3;

    private final EmailInvitacionService emailInvitacionService;
    private final long intervaloReintentoMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EnvioCorreoInvitacion(EmailInvitacionService emailInvitacionService,
                                 @Value("${invitacion.reintento-intervalo-ms:300000}") long intervaloReintentoMs) {
        this.emailInvitacionService = emailInvitacionService;
        this.intervaloReintentoMs = intervaloReintentoMs;
    }

    public void enviar(String email, String nombreEmpresa, String token) {
        intentar(email, nombreEmpresa, token, MAX_REINTENTOS);
    }

    private void intentar(String email, String nombreEmpresa, String token, int reintentosRestantes) {
        try {
            emailInvitacionService.enviarCorreoInvitacion(email, nombreEmpresa, token);
        } catch (Exception e) {
            log.error("Fallo el envio del correo de invitacion a {} (reintentos restantes: {})",
                    email, reintentosRestantes, e);
            if (reintentosRestantes > 0) {
                scheduler.schedule(
                        () -> intentar(email, nombreEmpresa, token, reintentosRestantes - 1),
                        intervaloReintentoMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    @PreDestroy
    void cerrar() {
        scheduler.shutdown();
    }
}
