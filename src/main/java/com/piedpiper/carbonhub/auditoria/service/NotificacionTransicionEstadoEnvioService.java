package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.repository.NotificacionTransicionAuditoriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Transiciones de estado de una notificacion, cada una en su propia transaccion corta.
 *
 * <p>{@code REQUIRES_NEW} en todas: el envio del correo ocurre fuera de cualquier transaccion y
 * estas escrituras tienen que confirmarse solas. Si compartieran la transaccion de quien las llama,
 * un fallo posterior en el lote borraria el hecho de que el correo ya salio y se enviaria dos
 * veces.</p>
 */
@Service
public class NotificacionTransicionEstadoEnvioService {

    public static final int INTENTOS_MAXIMOS = 3;

    private final NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository;

    public NotificacionTransicionEstadoEnvioService(
            NotificacionTransicionAuditoriaRepository notificacionTransicionAuditoriaRepository) {
        this.notificacionTransicionAuditoriaRepository = notificacionTransicionAuditoriaRepository;
    }

    /**
     * Toma la notificacion para enviarla, incrementando el contador de intentos. Devuelve
     * {@code false} si otro barrido se la llevo primero o si ya agoto los intentos: el {@code where}
     * del update es lo que decide, no una lectura previa.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reclamar(UUID notificacionId) {
        return notificacionTransicionAuditoriaRepository.reclamarParaEnvio(
                notificacionId, EstadoNotificacionTransicion.PENDIENTE, INTENTOS_MAXIMOS) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarEnviada(UUID notificacionId) {
        notificacionTransicionAuditoriaRepository.marcarEnviada(notificacionId,
                EstadoNotificacionTransicion.ENVIADA,
                EstadoNotificacionTransicion.PENDIENTE,
                Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallidaSiAgotoIntentos(UUID notificacionId) {
        notificacionTransicionAuditoriaRepository.marcarFallidaSiAgotoIntentos(notificacionId,
                EstadoNotificacionTransicion.FALLIDA,
                EstadoNotificacionTransicion.PENDIENTE,
                INTENTOS_MAXIMOS);
    }
}
