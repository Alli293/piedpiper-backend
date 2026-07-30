package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Registra el resultado del envio del correo de una alerta (PP-71). Vive en su propio bean porque lo
 * llama un servicio no transaccional despues de enviar: si fueran metodos de la misma clase, la
 * llamada interna no pasaria por el proxy de Spring y el {@code @Transactional} no aplicaria.
 */
@Service
public class AlertaEstadoEnvioService {

    /** Tope de intentos por alerta, segun el criterio de aceptacion de PP-71. */
    public static final int INTENTOS_MAXIMOS = 3;

    private static final Logger log = LoggerFactory.getLogger(AlertaEstadoEnvioService.class);

    private final AlertaRepository alertaRepository;

    public AlertaEstadoEnvioService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public void marcarEnviada(UUID alertaId) {
        alertaRepository.findById(alertaId).ifPresent(alerta -> {
            alerta.setEstado(EstadoAlerta.ENVIADA);
            alerta.setFechaEnvio(Instant.now());
            alerta.setIntentosEnvio(alerta.getIntentosEnvio() + 1);
            alertaRepository.save(alerta);
        });
    }

    /**
     * Suma un intento y, si con este se agotaron, deja la alerta en {@code FALLIDA}. Mientras queden
     * intentos se mantiene {@code PENDIENTE} para que el barrido la vuelva a tomar.
     */
    @Transactional
    public void registrarFallo(UUID alertaId) {
        alertaRepository.findById(alertaId).ifPresent(alerta -> {
            int intentos = alerta.getIntentosEnvio() + 1;
            alerta.setIntentosEnvio(intentos);
            if (intentos >= INTENTOS_MAXIMOS) {
                alerta.setEstado(EstadoAlerta.FALLIDA);
                log.error("Alerta {} marcada como fallida despues de {} intentos de envio",
                        alertaId, intentos);
            }
            alertaRepository.save(alerta);
        });
    }

    /**
     * Deja la alerta en {@code FALLIDA} sin reintentos, para los casos en que reintentar no puede
     * cambiar el resultado (por ejemplo un correo con formato invalido).
     */
    @Transactional
    public void marcarFallidaSinReintento(UUID alertaId) {
        alertaRepository.findById(alertaId).ifPresent(alerta -> {
            alerta.setEstado(EstadoAlerta.FALLIDA);
            alerta.setIntentosEnvio(INTENTOS_MAXIMOS);
            alertaRepository.save(alerta);
        });
    }
}
