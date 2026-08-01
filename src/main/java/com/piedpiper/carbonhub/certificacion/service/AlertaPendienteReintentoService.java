package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reintenta cada 5 minutos el correo de las alertas que quedaron pendientes (PP-71).
 *
 * <p>Los reintentos se apoyan en la tabla y no en una cola en memoria: si el proceso se reinicia
 * entre el fallo y el reintento, la alerta sigue en {@code PENDIENTE} y este barrido la vuelve a
 * tomar. Con una cola en memoria se perderia, y el proceso nocturno tampoco la regeneraria porque
 * la restriccion de unicidad hace que la alerta ya exista.</p>
 *
 * <p>Un fallo al notificar una alerta no detiene a las demas: cada una va en su propio try/catch.</p>
 *
 * <p>Que este barrido y el proceso nocturno puedan mirar la misma alerta no genera correos
 * duplicados, y no porque el scheduler tenga un solo hilo: lo garantiza el UPDATE condicional de
 * {@code AlertaRepository.reclamarParaEnvio}, que solo le entrega la fila a uno. Sigue siendo cierto
 * con el pool mas grande o con varias instancias corriendo en paralelo.</p>
 */
@Service
public class AlertaPendienteReintentoService {

    private static final Logger log = LoggerFactory.getLogger(AlertaPendienteReintentoService.class);

    private final AlertaRepository alertaRepository;
    private final AlertaVencimientoNotificacionService alertaVencimientoNotificacionService;

    public AlertaPendienteReintentoService(
            AlertaRepository alertaRepository,
            AlertaVencimientoNotificacionService alertaVencimientoNotificacionService) {
        this.alertaRepository = alertaRepository;
        this.alertaVencimientoNotificacionService = alertaVencimientoNotificacionService;
    }

    @Scheduled(fixedDelayString = "${certificaciones.alertas.reintento-intervalo-ms}")
    public void reintentarPendientes() {
        List<Alerta> pendientes = alertaRepository
                .findTop50ByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(
                        EstadoAlerta.PENDIENTE, AlertaEstadoEnvioService.INTENTOS_MAXIMOS);
        if (pendientes.isEmpty()) {
            return;
        }

        log.info("Reintentando el envio de {} alertas pendientes", pendientes.size());
        for (Alerta alerta : pendientes) {
            try {
                alertaVencimientoNotificacionService.notificar(alerta.getId());
            } catch (Exception e) {
                log.error("Error al reintentar la notificacion de la alerta {}", alerta.getId(), e);
            }
        }
    }
}
