package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Unica fuente de verdad de las transiciones de estado de una solicitud de auditoria.
 *
 * <p>La tabla es exhaustiva: una combinacion de estado y evento que no este aca no es valida y no
 * existe forma de aplicarla. Que viva en un solo lugar es lo que permite que cada historia que
 * agrega un paso al flujo declare su transicion aca en vez de repartir {@code setEstado} por los
 * servicios, donde nadie puede revisar el conjunto completo.</p>
 *
 * <p>Cada entrada tambien fija quien puede originar el evento. La autorizacion por rol vive en el
 * controlador, pero eso solo dice que el usuario es un auditor; lo que se valida aca es que el
 * evento venga del tipo de actor correcto para esa transicion, que es una regla del flujo y no de
 * seguridad.</p>
 */
@Component
public class ValidadorTransicionAuditoria {

    private record Clave(EstadoSolicitudAuditoria origen, EventoTransicionAuditoria evento) {
    }

    private record Destino(EstadoSolicitudAuditoria estado, ActorTransicionAuditoria actor) {
    }

    private static final Map<Clave, Destino> TABLA = Map.ofEntries(
            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, EventoTransicionAuditoria.AUDITOR_ACEPTA),
                    new Destino(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.AUDITOR_ASIGNADO, EventoTransicionAuditoria.INICIO_REVISION),
                    new Destino(EstadoSolicitudAuditoria.EN_REVISION, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.EN_REVISION, EventoTransicionAuditoria.REPORTE_CARGADO),
                    new Destino(EstadoSolicitudAuditoria.REPORTE_CARGADO, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.REPORTE_CARGADO, EventoTransicionAuditoria.RESULTADO_APROBADA),
                    new Destino(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.REPORTE_CARGADO, EventoTransicionAuditoria.RESULTADO_OBSERVACIONES),
                    new Destino(EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, EventoTransicionAuditoria.AUDITOR_RECHAZA),
                    new Destino(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, ActorTransicionAuditoria.AUDITOR)),

            Map.entry(
                    new Clave(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, EventoTransicionAuditoria.VENCIDA_POR_NO_RESPUESTA),
                    new Destino(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA, ActorTransicionAuditoria.SISTEMA)));

    /**
     * Devuelve el estado destino de una transicion valida, o lanza 422 si la combinacion no existe
     * en la tabla o el actor no es el autorizado para ese evento.
     */
    public EstadoSolicitudAuditoria destinoDe(EstadoSolicitudAuditoria origen,
                                              EventoTransicionAuditoria evento,
                                              ActorTransicionAuditoria actor) {
        Destino destino = TABLA.get(new Clave(origen, evento));
        if (destino == null || destino.actor() != actor) {
            throw ApiException.transicionAuditoriaInvalida();
        }
        return destino.estado();
    }

    public Optional<EstadoSolicitudAuditoria> destinoPermitido(EstadoSolicitudAuditoria origen,
                                                              EventoTransicionAuditoria evento) {
        return Optional.ofNullable(TABLA.get(new Clave(origen, evento))).map(Destino::estado);
    }
}
