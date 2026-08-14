package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Aplica una transicion de estado y deja su registro en el historial.
 *
 * <p>Es el unico camino por el que el estado de una solicitud puede cambiar. Los servicios de cada
 * historia describen que evento ocurrio y quien lo origino; el estado destino lo decide
 * {@link ValidadorTransicionAuditoria} a partir de la tabla, no quien llama. Asi es imposible que un
 * servicio nuevo invente una transicion que la tabla no contempla, ni que aplique el cambio sin
 * dejar rastro.</p>
 *
 * <p>Sin {@code REQUIRES_NEW} a proposito: los metodos llevan {@code @Transactional} con la
 * propagacion por defecto, asi que se suman a la transaccion del servicio que los invoca y el
 * cambio de estado y su registro en el historial quedan atomicos. Con una transaccion propia, el
 * historial podria quedar registrando una transicion que despues se revierte.</p>
 */
@Service
public class TransicionEstadoAuditoriaService {

    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    private final ValidadorTransicionAuditoria validadorTransicionAuditoria;
    private final NotificacionTransicionRegistroService notificacionTransicionRegistroService;

    public TransicionEstadoAuditoriaService(
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            ValidadorTransicionAuditoria validadorTransicionAuditoria,
            NotificacionTransicionRegistroService notificacionTransicionRegistroService) {
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.validadorTransicionAuditoria = validadorTransicionAuditoria;
        this.notificacionTransicionRegistroService = notificacionTransicionRegistroService;
    }

    @Transactional
    public void aplicar(SolicitudAuditoria solicitud,
                        EventoTransicionAuditoria evento,
                        ActorTransicionAuditoria actor,
                        Usuario responsable) {
        EstadoSolicitudAuditoria origen = solicitud.getEstado();
        EstadoSolicitudAuditoria destino = validadorTransicionAuditoria.destinoDe(origen, evento, actor);

        solicitud.setEstado(destino);
        registrar(solicitud, origen, destino, evento, actor, responsable);
    }

    /**
     * Registra el nacimiento de la solicitud. Va aparte de {@link #aplicar} porque no hay estado de
     * origen del cual partir: la fila de la tabla dice "(creacion)", no un estado previo. Se anota
     * como origen el mismo estado inicial para no dejar la columna nula en una tabla de auditoria.
     */
    @Transactional
    public void registrarCreacion(SolicitudAuditoria solicitud, Usuario responsable) {
        registrar(solicitud,
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.SOLICITUD_CREADA,
                ActorTransicionAuditoria.EMPRESA,
                responsable);
    }

    private void registrar(SolicitudAuditoria solicitud,
                           EstadoSolicitudAuditoria origen,
                           EstadoSolicitudAuditoria destino,
                           EventoTransicionAuditoria evento,
                           ActorTransicionAuditoria actor,
                           Usuario responsable) {
        transicionEstadoAuditoriaRepository.save(TransicionEstadoAuditoria.builder()
                .solicitud(solicitud)
                .estadoAnterior(origen)
                .estadoNuevo(destino)
                .evento(evento)
                .actor(actor)
                .responsableId(responsable == null ? null : responsable.getId())
                .responsableNombre(Usuario.recortarNombre(
                        responsable == null ? null : responsable.nombreCompleto()))
                .fecha(Instant.now())
                .build());

        notificacionTransicionRegistroService.encolar(solicitud, origen, destino, evento);
    }
}
