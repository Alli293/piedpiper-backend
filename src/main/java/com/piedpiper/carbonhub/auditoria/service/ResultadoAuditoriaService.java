package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.ResultadoAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ResultadoAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class ResultadoAuditoriaService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    private final TransicionEstadoAuditoriaService transicionEstadoAuditoriaService;
    private final EmisionCertificacionPort emisionCertificacionPort;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;
    private final TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper;

    public ResultadoAuditoriaService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            TransicionEstadoAuditoriaService transicionEstadoAuditoriaService,
            EmisionCertificacionPort emisionCertificacionPort,
            SolicitudAuditoriaMapper solicitudAuditoriaMapper,
            TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.transicionEstadoAuditoriaService = transicionEstadoAuditoriaService;
        this.emisionCertificacionPort = emisionCertificacionPort;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
        this.transicionEstadoAuditoriaMapper = transicionEstadoAuditoriaMapper;
    }

    @Transactional
    public SolicitudAuditoriaDetalleResponseDTO emitir(UUID solicitudId,
                                                       ResultadoAuditoriaRequestDTO datos,
                                                       UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);
        Usuario auditor = validarAuditorAsignado(solicitud, usuarioId);
        validarEstadoPermiteResultado(solicitud);

        ResultadoAuditoria resultado = ResultadoAuditoria.desde(datos.getResultado())
                .orElseThrow(ApiException::resultadoAuditoriaInvalido);

        if (resultado == ResultadoAuditoria.APROBADA) {
            aplicar(solicitud, EventoTransicionAuditoria.RESULTADO_APROBADA, auditor);
            SolicitudAuditoria guardada = guardar(solicitud);
            emitirCertificacionTrasCommit(comandoEmision(guardada, auditor));
            return detalleDe(guardada);
        }

        aplicar(solicitud, EventoTransicionAuditoria.RESULTADO_OBSERVACIONES, auditor);
        return detalleDe(guardar(solicitud));
    }

    private Usuario validarAuditorAsignado(SolicitudAuditoria solicitud, UUID usuarioId) {
        Usuario auditor = solicitud.getAuditor();
        if (auditor == null || !auditor.getId().equals(usuarioId)) {
            throw ApiException.resultadoAuditoriaAjena();
        }
        return auditor;
    }

    private void validarEstadoPermiteResultado(SolicitudAuditoria solicitud) {
        if (solicitud.getEstado() != EstadoSolicitudAuditoria.REPORTE_CARGADO
                || solicitud.getReporteAuditoria() == null
                || solicitud.getFechaAuditoriaRealizada() == null) {
            throw ApiException.resultadoAuditoriaNoDisponible();
        }
    }

    private void aplicar(SolicitudAuditoria solicitud,
                         EventoTransicionAuditoria evento,
                         Usuario auditor) {
        transicionEstadoAuditoriaService.aplicar(
                solicitud, evento, ActorTransicionAuditoria.AUDITOR, auditor);
    }

    private EmitirCertificacionRequestDTO comandoEmision(SolicitudAuditoria solicitud, Usuario auditor) {
        return new EmitirCertificacionRequestDTO(
                solicitud.getId(),
                solicitud.getEmpresa().getId(),
                auditor.getId(),
                ResultadoAuditoria.APROBADA.getCodigo(),
                solicitud.getFechaAuditoriaRealizada(),
                TipoCertificacion.INVENTARIO_GEI,
                null);
    }

    private void emitirCertificacionTrasCommit(EmitirCertificacionRequestDTO comando) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emisionCertificacionPort.emitirPorAuditoriaAprobada(comando);
                }
            });
        } else {
            emisionCertificacionPort.emitirPorAuditoriaAprobada(comando);
        }
    }

    private SolicitudAuditoria guardar(SolicitudAuditoria solicitud) {
        try {
            return solicitudAuditoriaRepository.saveAndFlush(solicitud);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.resultadoAuditoriaNoDisponible();
        }
    }

    private SolicitudAuditoriaDetalleResponseDTO detalleDe(SolicitudAuditoria solicitud) {
        SolicitudAuditoriaDetalleResponseDTO detalle = solicitudAuditoriaMapper.toDetalleDto(solicitud);
        detalle.setHistorial(transicionEstadoAuditoriaMapper.toDtos(
                transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(solicitud.getId())));
        return detalle;
    }
}
