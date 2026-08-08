package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.models.dtos.DecisionAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.DecisionAuditor;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class DecisionAuditorService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransicionEstadoAuditoriaService transicionEstadoAuditoriaService;
    private final long horasParaResponder;

    public DecisionAuditorService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            TransicionEstadoAuditoriaService transicionEstadoAuditoriaService,
            @Value("${auditoria.expiracion-asignacion-horas:120}") long horasParaResponder) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transicionEstadoAuditoriaService = transicionEstadoAuditoriaService;
        this.horasParaResponder = horasParaResponder;
    }

    @Transactional
    public void responder(UUID solicitudId, DecisionAuditorRequestDTO datos, UUID usuarioId) {
        DecisionAuditor decision = DecisionAuditor.desde(datos.getDecision())
                .orElseThrow(ApiException::decisionAuditorInvalida);

        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);

        Usuario auditor = validarAsignacionPendiente(solicitud, usuarioId);

        // Un unico instante para toda la decision: con varios Instant.now() el plazo podia validarse
        // contra un momento y la fecha guardarse con otro, y en el borde de las 120 horas eso decide
        // si la respuesta pasa o no por milisegundos.
        Instant ahora = Instant.now();

        if (decision == DecisionAuditor.ACEPTADA) {
            aceptar(solicitud, auditor, ahora);
        } else {
            rechazar(solicitud, auditor, datos.getMotivoRechazo(), ahora);
        }

        guardar(solicitud);
    }

    /**
     * La aceptacion aplica las dos transiciones dentro de la misma transaccion de base: la historia
     * pide que aceptar deje la solicitud en revision, no en un estado intermedio visible. Van como
     * dos entradas del historial y no como una sola porque son dos hechos distintos, y la tabla de
     * transiciones no admite un salto directo de solicitud_enviada a en_revision.
     */
    private void aceptar(SolicitudAuditoria solicitud, Usuario auditor, Instant ahora) {
        validarPlazo(solicitud, ahora);

        transicionEstadoAuditoriaService.aplicar(solicitud,
                EventoTransicionAuditoria.AUDITOR_ACEPTA, ActorTransicionAuditoria.AUDITOR, auditor);
        transicionEstadoAuditoriaService.aplicar(solicitud,
                EventoTransicionAuditoria.INICIO_REVISION, ActorTransicionAuditoria.AUDITOR, auditor);

        solicitud.setFechaAceptacion(ahora);
    }

    /**
     * El rechazo no valida el plazo a proposito: la historia solo se lo exige a la aceptacion, y si
     * el plazo ya vencio el proceso automatico habria liberado la asignacion, con lo cual la
     * validacion de asignacion pendiente ya corta antes. Exigirlo aca rechazaria una respuesta
     * legitima que llego justo antes de que corriera el barrido.
     *
     * <p>La transicion se registra antes de soltar la asignacion para que el auditor todavia figure
     * como destinatario de la notificacion: es la confirmacion de que su respuesta quedo
     * registrada.</p>
     */
    private void rechazar(SolicitudAuditoria solicitud, Usuario auditor, String motivo, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw ApiException.motivoRechazoRequerido();
        }

        transicionEstadoAuditoriaService.aplicar(solicitud,
                EventoTransicionAuditoria.AUDITOR_RECHAZA, ActorTransicionAuditoria.AUDITOR, auditor);

        solicitud.setMotivoRechazo(motivo.trim());
        solicitud.setFechaRechazo(ahora);
        solicitud.setAuditor(null);
        solicitud.setOrigenAsignacion(null);
        solicitud.setFechaAsignacion(null);
    }

    /**
     * Distingue los dos casos a proposito: si hay otro auditor asignado es un intento de responder
     * por una solicitud ajena (403), mientras que si ya no hay asignacion pendiente es que la
     * solicitud dejo de estar disponible para este auditor (409). Devolver lo mismo en ambos casos
     * dejaria al auditor sin saber si se equivoco de solicitud o si simplemente llego tarde.
     */
    private Usuario validarAsignacionPendiente(SolicitudAuditoria solicitud, UUID usuarioId) {
        Usuario asignado = solicitud.getAuditor();
        if (asignado == null) {
            throw ApiException.decisionAuditorNoDisponible();
        }
        if (!asignado.getId().equals(usuarioId)) {
            throw ApiException.decisionAuditorAjena();
        }
        if (solicitud.getFechaAceptacion() != null) {
            throw ApiException.decisionAuditorNoDisponible();
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
    }

    private void validarPlazo(SolicitudAuditoria solicitud, Instant ahora) {
        Instant asignacion = solicitud.getFechaAsignacion();
        if (asignacion == null
                || Duration.between(asignacion, ahora).toHours() >= horasParaResponder) {
            throw ApiException.decisionAuditorNoDisponible();
        }
    }

    /**
     * Traduce la carrera al mismo 409 que la validacion: dos respuestas simultaneas sobre la misma
     * solicitud (o una respuesta que llega mientras el barrido la libera) pasan las dos la
     * validacion, y el {@code @Version} de la entidad hace fallar a la perdedora al escribir.
     */
    private void guardar(SolicitudAuditoria solicitud) {
        try {
            solicitudAuditoriaRepository.saveAndFlush(solicitud);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.decisionAuditorNoDisponible();
        }
    }
}
