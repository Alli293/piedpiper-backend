package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.NotificacionTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.repository.NotificacionTransicionAuditoriaRepository;
import com.piedpiper.carbonhub.notification.service.EmailTransicionAuditoriaService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Barrido que envia las notificaciones de cambio de estado pendientes y reintenta las que fallaron.
 *
 * <p><strong>Sin {@code @Transactional} a proposito.</strong> El envio del correo es una llamada de
 * red que puede tardar; tenerla dentro de una transaccion mantendria una conexion de base tomada
 * todo ese tiempo. Cada escritura de estado se hace en su propia transaccion corta a traves de
 * {@link NotificacionTransicionEstadoEnvioService}.</p>
 *
 * <p>El orden importa: primero se reclama (lo que ya cuenta el intento) y solo despues se envia. Al
 * reves, un fallo del proceso entre el envio y el conteo dejaria la notificacion pendiente con el
 * correo ya entregado, y el siguiente barrido lo mandaria de nuevo.</p>
 */
@Service
public class NotificacionTransicionEnvioService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransicionEnvioService.class);

    private final NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository;
    private final NotificacionTransicionEstadoEnvioService notificacionTransicionEstadoEnvioService;
    private final EmailTransicionAuditoriaService emailTransicionAuditoriaService;
    private final String urlBaseDetalle;

    public NotificacionTransicionEnvioService(
            NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository,
            NotificacionTransicionEstadoEnvioService notificacionTransicionEstadoEnvioService,
            EmailTransicionAuditoriaService emailTransicionAuditoriaService,
            @Value("${app.frontend-url:http://localhost:4200}") String urlBaseDetalle) {
        this.notificacionTransicionAuditoriaRepository = notificacionTransicionAuditoriaRepository;
        this.notificacionTransicionEstadoEnvioService = notificacionTransicionEstadoEnvioService;
        this.emailTransicionAuditoriaService = emailTransicionAuditoriaService;
        this.urlBaseDetalle = urlBaseDetalle;
    }

    @Scheduled(fixedDelayString = "${auditoria.notificaciones.reintento-intervalo-ms:300000}")
    public void enviarPendientes() {
        List<NotificacionTransicionAuditoria> pendientes = notificacionTransicionAuditoriaRepository
                .findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaCreacionAsc(
                        EstadoNotificacionTransicion.PENDIENTE,
                        NotificacionTransicionEstadoEnvioService.INTENTOS_MAXIMOS);

        pendientes.forEach(this::enviarAislada);
    }

    /**
     * Cada notificacion se procesa aislada para que una que falle no corte el lote: la historia pide
     * que las que quedan sin procesar se evaluen en la siguiente ejecucion sin omitirse.
     */
    private void enviarAislada(NotificacionTransicionAuditoria notificacion) {
        UUID notificacionId = notificacion.getId();
        try {
            enviar(notificacion);
        } catch (RuntimeException e) {
            log.error("Fallo el envio de la notificacion de cambio de estado {} de la solicitud {}",
                    notificacionId, notificacion.getSolicitud().getId(), e);
            notificacionTransicionEstadoEnvioService.marcarFallidaSiAgotoIntentos(notificacionId);
        }
    }

    private void enviar(NotificacionTransicionAuditoria notificacion) {
        UUID notificacionId = notificacion.getId();
        if (!notificacionTransicionEstadoEnvioService.reclamar(notificacionId)) {
            return;
        }

        emailTransicionAuditoriaService.enviarCambioEstado(
                notificacion.getDestinatarioEmail(),
                notificacion.getDestinatarioNombre(),
                notificacion.getNombreEmpresa(),
                notificacion.getEstadoNuevo().getDescripcion(),
                "%s/empresa/auditorias/%s".formatted(urlBaseDetalle, notificacion.getSolicitud().getId()));

        notificacionTransicionEstadoEnvioService.marcarEnviada(notificacionId);
    }
}
