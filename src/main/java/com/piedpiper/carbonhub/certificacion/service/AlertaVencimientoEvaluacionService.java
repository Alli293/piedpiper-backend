package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.certificacion.repository.AlertaRepository;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Evalua una unica certificacion contra los umbrales de vencimiento (90, 30 y
 * 7 dias) y registra las alertas que correspondan. Invocada por
 * {@link AlertaVencimientoSchedulerService} una vez por certificacion activa.
 *
 * <p>Recibe el id y recarga la certificacion dentro de su propia transaccion
 * (en vez de recibir la entidad ya cargada por el scheduler) para no
 * arrastrar un proxy LAZY de una sesion ya cerrada, y para trabajar siempre
 * con el estado mas reciente de la certificacion.</p>
 */
@Service
public class AlertaVencimientoEvaluacionService {

    private static final Logger log = LoggerFactory.getLogger(AlertaVencimientoEvaluacionService.class);

    private final CertificacionRepository certificacionRepository;
    private final AlertaRepository alertaRepository;

    public AlertaVencimientoEvaluacionService(CertificacionRepository certificacionRepository,
                                              AlertaRepository alertaRepository) {
        this.certificacionRepository = certificacionRepository;
        this.alertaRepository = alertaRepository;
    }

    /**
     * Genera todas las alertas de vencimiento pendientes para la
     * certificacion dada. Un umbral se considera "alcanzado" cuando los dias
     * restantes son menores o iguales a los dias del umbral (no solo cuando
     * coinciden exactamente): si el proceso no corrio la noche exacta en que
     * la certificacion cruzo un umbral (deploy, reinicio, caida puntual),
     * igual se genera al correr de nuevo, en vez de perderse para siempre.
     * La unique constraint {@code (certificacion_id, tipo_alerta)} mas el
     * {@code existsBy} de abajo garantizan que esto no duplique alertas ya
     * generadas.
     *
     * @return las alertas nuevas generadas en esta corrida, en el orden de
     *         {@link TipoAlerta} (puede ser mas de una si se "recupera" mas
     *         de un umbral perdido de una sola vez); vacio si no correspondia
     *         generar ninguna.
     */
    @Transactional
    public List<Alerta> evaluar(UUID certificacionId) {
        Certificacion certificacion = certificacionRepository.findById(certificacionId).orElse(null);
        if (certificacion == null || certificacion.getEstado() != EstadoCertificacion.ACTIVA) {
            return List.of();
        }

        long diasRestantes = ChronoUnit.DAYS.between(
                LocalDate.now(ZonasHorarias.COSTA_RICA), certificacion.getFechaVencimiento());

        List<Alerta> generadas = new ArrayList<>();
        for (TipoAlerta tipoAlerta : TipoAlerta.values()) {
            if (diasRestantes > tipoAlerta.getDias()) {
                continue;
            }
            if (alertaRepository.existsByCertificacionIdAndTipoAlerta(certificacion.getId(), tipoAlerta)) {
                continue;
            }

            Alerta alerta = alertaRepository.save(Alerta.builder()
                    .empresa(certificacion.getEmpresa())
                    .certificacion(certificacion)
                    .tipoAlerta(tipoAlerta)
                    .estado(EstadoAlerta.PENDIENTE)
                    .fechaGeneracion(Instant.now())
                    .build());
            generadas.add(alerta);

            log.info("Alerta {} generada para la certificacion {}", tipoAlerta, certificacion.getId());
        }

        return generadas;
    }
}
