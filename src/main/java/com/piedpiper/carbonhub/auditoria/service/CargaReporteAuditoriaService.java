package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.ContenidoReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.ReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.ContenidoReporteAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class CargaReporteAuditoriaService {

    private static final ZoneId ZONA_HORARIA_NEGOCIO = ZoneId.of("America/Costa_Rica");

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final ContenidoReporteAuditoriaRepository contenidoReporteAuditoriaRepository;
    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    private final ValidadorReporteAuditoriaPdf validadorReporteAuditoriaPdf;
    private final ReporteAuditoriaFactory reporteAuditoriaFactory;
    private final TransicionEstadoAuditoriaService transicionEstadoAuditoriaService;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;
    private final TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper;

    public CargaReporteAuditoriaService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            ContenidoReporteAuditoriaRepository contenidoReporteAuditoriaRepository,
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            ValidadorReporteAuditoriaPdf validadorReporteAuditoriaPdf,
            ReporteAuditoriaFactory reporteAuditoriaFactory,
            TransicionEstadoAuditoriaService transicionEstadoAuditoriaService,
            SolicitudAuditoriaMapper solicitudAuditoriaMapper,
            TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.contenidoReporteAuditoriaRepository = contenidoReporteAuditoriaRepository;
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.validadorReporteAuditoriaPdf = validadorReporteAuditoriaPdf;
        this.reporteAuditoriaFactory = reporteAuditoriaFactory;
        this.transicionEstadoAuditoriaService = transicionEstadoAuditoriaService;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
        this.transicionEstadoAuditoriaMapper = transicionEstadoAuditoriaMapper;
    }

    @Transactional
    public SolicitudAuditoriaDetalleResponseDTO cargar(UUID solicitudId,
                                                       MultipartFile reporteAuditoria,
                                                       LocalDate fechaAuditoriaRealizada,
                                                       UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);

        Usuario auditor = validarAuditorAsignado(solicitud, usuarioId);
        boolean transiciona = validarEstadoPermiteCarga(solicitud);
        byte[] contenido = validadorReporteAuditoriaPdf.validar(reporteAuditoria);
        validarFechaAuditoria(solicitud, fechaAuditoriaRealizada);

        UUID reporteAnteriorId = reporteAnteriorId(solicitud);
        if (reporteAnteriorId != null) {
            contenidoReporteAuditoriaRepository.deleteByReporteAuditoriaId(reporteAnteriorId);
        }

        Instant ahora = Instant.now();
        ReporteAuditoria reporte = reporteAuditoriaFactory.crear(reporteAuditoria, ahora);
        solicitud.reemplazarReporteAuditoria(reporte);
        solicitud.setFechaAuditoriaRealizada(fechaAuditoriaRealizada);
        solicitud.setFechaCargaReporte(ahora);

        if (transiciona) {
            transicionEstadoAuditoriaService.aplicar(solicitud,
                    EventoTransicionAuditoria.REPORTE_CARGADO,
                    ActorTransicionAuditoria.AUDITOR,
                    auditor);
        }

        SolicitudAuditoria guardada = guardar(solicitud);
        guardarContenido(guardada.getReporteAuditoria(), contenido);
        return detalleDe(guardada);
    }

    private Usuario validarAuditorAsignado(SolicitudAuditoria solicitud, UUID usuarioId) {
        Usuario auditor = solicitud.getAuditor();
        if (auditor == null || !auditor.getId().equals(usuarioId)) {
            throw ApiException.cargaReporteAuditoriaAjena();
        }
        return auditor;
    }

    private boolean validarEstadoPermiteCarga(SolicitudAuditoria solicitud) {
        if (solicitud.getEstado() == EstadoSolicitudAuditoria.EN_REVISION) {
            return true;
        }
        if (solicitud.getEstado() == EstadoSolicitudAuditoria.REPORTE_CARGADO) {
            return false;
        }
        throw ApiException.cargaReporteAuditoriaNoDisponible();
    }

    private void validarFechaAuditoria(SolicitudAuditoria solicitud, LocalDate fechaAuditoriaRealizada) {
        if (fechaAuditoriaRealizada == null || solicitud.getFechaAceptacion() == null) {
            throw ApiException.fechaAuditoriaRealizadaInvalida();
        }
        LocalDate fechaAceptacion = solicitud.getFechaAceptacion()
                .atZone(ZONA_HORARIA_NEGOCIO)
                .toLocalDate();
        LocalDate hoyServidor = LocalDate.now(ZONA_HORARIA_NEGOCIO);
        if (fechaAuditoriaRealizada.isBefore(fechaAceptacion)
                || fechaAuditoriaRealizada.isAfter(hoyServidor)) {
            throw ApiException.fechaAuditoriaRealizadaInvalida();
        }
    }

    private SolicitudAuditoria guardar(SolicitudAuditoria solicitud) {
        try {
            return solicitudAuditoriaRepository.saveAndFlush(solicitud);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ApiException.cargaReporteAuditoriaNoDisponible();
        }
    }

    private void guardarContenido(ReporteAuditoria reporte, byte[] contenido) {
        contenidoReporteAuditoriaRepository.saveAndFlush(ContenidoReporteAuditoria.builder()
                .reporteAuditoria(reporte)
                .contenido(contenido)
                .build());
    }

    private UUID reporteAnteriorId(SolicitudAuditoria solicitud) {
        return solicitud.getReporteAuditoria() == null ? null : solicitud.getReporteAuditoria().getId();
    }

    private SolicitudAuditoriaDetalleResponseDTO detalleDe(SolicitudAuditoria solicitud) {
        SolicitudAuditoriaDetalleResponseDTO detalle = solicitudAuditoriaMapper.toDetalleDto(solicitud);
        detalle.setHistorial(transicionEstadoAuditoriaMapper.toDtos(
                transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(solicitud.getId())));
        return detalle;
    }
}
