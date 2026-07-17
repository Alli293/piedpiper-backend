package com.piedpiper.carbonhub.validacion.service;

import com.piedpiper.carbonhub.notification.service.EmailValidacionAuditorService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class EnvioCorreoValidacionService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoValidacionService.class);
    private static final int MAX_REINTENTOS = 3;

    private final EmailValidacionAuditorService emailValidacionAuditorService;
    private final long intervaloReintentoMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public EnvioCorreoValidacionService(EmailValidacionAuditorService emailValidacionAuditorService,
                                        @Value("${validacion.reintento-intervalo-ms:300000}") long intervaloReintentoMs) {
        this.emailValidacionAuditorService = emailValidacionAuditorService;
        this.intervaloReintentoMs = intervaloReintentoMs;
    }

    public void enviar(String nombre, String email, boolean aprobado, String motivoRechazo) {
        intentar(nombre, email, aprobado, motivoRechazo, MAX_REINTENTOS);
    }

    private void intentar(String nombre, String email, boolean aprobado, String motivoRechazo,
                          int reintentosRestantes) {
        try {
            emailValidacionAuditorService.enviarResultadoValidacion(nombre, email, aprobado, motivoRechazo);
        } catch (Exception e) {
            log.error("Fallo el envio del correo de validacion a {} (reintentos restantes: {})",
                    email, reintentosRestantes, e);
            if (reintentosRestantes > 0) {
                scheduler.schedule(
                        () -> intentar(nombre, email, aprobado, motivoRechazo, reintentosRestantes - 1),
                        intervaloReintentoMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    @PreDestroy
    void cerrar() {
        scheduler.shutdown();
    }
}
