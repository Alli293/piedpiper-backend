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
 * Maneja el estado de envio de una alerta (PP-71).
 *
 * <p>Todas las transiciones son sentencias condicionales de una sola pasada, no leer-modificar-
 * escribir: la condicion viaja en el {@code where} y la base es la que decide si aplica. Asi dos
 * procesos que miren la misma alerta no pueden mandar el correo dos veces ni pisarse el contador de
 * intentos, sin depender de que el scheduler corra en un solo hilo ni en una sola instancia.</p>
 *
 * <p>Vive en su propio bean porque lo llama un servicio no transaccional: si fueran metodos de la
 * misma clase, la llamada interna no pasaria por el proxy de Spring y el {@code @Transactional} no
 * aplicaria.</p>
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

    /**
     * Intenta tomar la alerta para enviarla. Solo uno gana: quien recibe {@code true} es el unico
     * autorizado a mandar el correo.
     *
     * @return {@code true} si la reclamo, {@code false} si ya no estaba pendiente o agoto intentos
     */
    @Transactional
    public boolean reclamar(UUID alertaId) {
        return alertaRepository.reclamarParaEnvio(alertaId, EstadoAlerta.PENDIENTE, INTENTOS_MAXIMOS) == 1;
    }

    @Transactional
    public void marcarEnviada(UUID alertaId) {
        alertaRepository.marcarEnviada(alertaId, EstadoAlerta.ENVIADA, EstadoAlerta.PENDIENTE, Instant.now());
    }

    /**
     * Cierra la alerta como fallida solo si el intento que acaba de fallar era el ultimo. Si le
     * quedan intentos se queda pendiente y el barrido la retoma.
     */
    @Transactional
    public void registrarFallo(UUID alertaId) {
        int cerradas = alertaRepository.marcarFallidaSiAgotoIntentos(
                alertaId, EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, INTENTOS_MAXIMOS);
        if (cerradas == 1) {
            log.error("Alerta {} marcada como fallida despues de {} intentos de envio",
                    alertaId, INTENTOS_MAXIMOS);
        }
    }

    /**
     * Deja la alerta en {@code FALLIDA} sin reintentos, para los casos en que reintentar no puede
     * cambiar el resultado (por ejemplo un correo con formato invalido).
     */
    @Transactional
    public void marcarFallidaSinReintento(UUID alertaId) {
        alertaRepository.marcarFallidaDefinitiva(
                alertaId, EstadoAlerta.FALLIDA, EstadoAlerta.PENDIENTE, INTENTOS_MAXIMOS);
    }
}
