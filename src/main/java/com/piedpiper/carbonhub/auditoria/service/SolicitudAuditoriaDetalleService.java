package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SolicitudAuditoriaDetalleService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;
    private final TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper;
    private final AccesoSolicitudAuditoria accesoSolicitudAuditoria;

    public SolicitudAuditoriaDetalleService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            SolicitudAuditoriaMapper solicitudAuditoriaMapper,
            TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper,
            AccesoSolicitudAuditoria accesoSolicitudAuditoria) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
        this.transicionEstadoAuditoriaMapper = transicionEstadoAuditoriaMapper;
        this.accesoSolicitudAuditoria = accesoSolicitudAuditoria;
    }

    @Transactional(readOnly = true)
    public SolicitudAuditoriaDetalleResponseDTO obtenerDetalle(UUID solicitudId, UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (!accesoSolicitudAuditoria.puedeConsultar(solicitud, usuario)) {
            throw ApiException.solicitudAuditoriaAjena();
        }

        SolicitudAuditoriaDetalleResponseDTO detalle = solicitudAuditoriaMapper.toDetalleDto(solicitud);
        detalle.setHistorial(transicionEstadoAuditoriaMapper.toDtos(
                transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(solicitudId)));
        return detalle;
    }
}
