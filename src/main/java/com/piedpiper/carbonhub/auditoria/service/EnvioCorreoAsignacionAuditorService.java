package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.notification.service.EmailAsignacionAuditorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class EnvioCorreoAsignacionAuditorService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoAsignacionAuditorService.class);
    private static final int MAX_REINTENTOS = 3;

    private enum TipoCorreo {
        ASIGNACION,
        EXPIRACION
    }

    private record Envio(TipoCorreo tipo,
                         String destinatario,
                         String nombreEmpresa,
                         String nombreAuditor,
                         int reintentosRestantes) {

        Envio siguienteIntento() {
            return new Envio(tipo, destinatario, nombreEmpresa, nombreAuditor, reintentosRestantes - 1);
        }
    }

    private final EmailAsignacionAuditorService emailAsignacionAuditorService;
    private final Queue<Envio> pendientes = new ConcurrentLinkedQueue<>();

    public EnvioCorreoAsignacionAuditorService(EmailAsignacionAuditorService emailAsignacionAuditorService) {
        this.emailAsignacionAuditorService = emailAsignacionAuditorService;
    }

    public void enviarAsignacion(String nombreAuditor, String emailAuditor, String nombreEmpresa) {
        intentar(new Envio(TipoCorreo.ASIGNACION, emailAuditor, nombreEmpresa, nombreAuditor, MAX_REINTENTOS));
    }

    public void enviarExpiracion(String correoEmpresa, String nombreEmpresa, String nombreAuditor) {
        intentar(new Envio(TipoCorreo.EXPIRACION, correoEmpresa, nombreEmpresa, nombreAuditor, MAX_REINTENTOS));
    }

    @Scheduled(fixedDelayString = "${auditoria.reintento-intervalo-ms:300000}")
    public void reintentarPendientes() {
        List<Envio> lote = new ArrayList<>();
        for (Envio envio = pendientes.poll(); envio != null; envio = pendientes.poll()) {
            lote.add(envio);
        }
        lote.forEach(this::intentar);
    }

    private void intentar(Envio envio) {
        try {
            switch (envio.tipo()) {
                case ASIGNACION -> emailAsignacionAuditorService.enviarAsignacion(
                        envio.nombreAuditor(), envio.destinatario(), envio.nombreEmpresa());
                case EXPIRACION -> emailAsignacionAuditorService.enviarExpiracionAsignacion(
                        envio.destinatario(), envio.nombreEmpresa(), envio.nombreAuditor());
            }
        } catch (Exception e) {
            log.error("Fallo el envio del correo de {} de auditor a {} (reintentos restantes: {})",
                    envio.tipo(), envio.destinatario(), envio.reintentosRestantes(), e);
            if (envio.reintentosRestantes() > 0) {
                pendientes.add(envio.siguienteIntento());
            }
        }
    }
}
