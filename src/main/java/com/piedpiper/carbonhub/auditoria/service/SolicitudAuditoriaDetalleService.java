package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
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

    public SolicitudAuditoriaDetalleService(
            SolicitudAuditoriaRepository solicitudAuditoriaRepository,
            TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository,
            UsuarioRepository usuarioRepository,
            SolicitudAuditoriaMapper solicitudAuditoriaMapper,
            TransicionEstadoAuditoriaMapper transicionEstadoAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.transicionEstadoAuditoriaRepository = transicionEstadoAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
        this.transicionEstadoAuditoriaMapper = transicionEstadoAuditoriaMapper;
    }

    @Transactional(readOnly = true)
    public SolicitudAuditoriaDetalleResponseDTO obtenerDetalle(UUID solicitudId, UUID usuarioId) {
        SolicitudAuditoria solicitud = solicitudAuditoriaRepository.findById(solicitudId)
                .orElseThrow(ApiException::solicitudAuditoriaNoEncontrada);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));

        if (!puedeConsultar(solicitud, usuario)) {
            throw ApiException.solicitudAuditoriaAjena();
        }

        SolicitudAuditoriaDetalleResponseDTO detalle = solicitudAuditoriaMapper.toDetalleDto(solicitud);
        detalle.setHistorial(transicionEstadoAuditoriaMapper.toDtos(
                transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(solicitudId)));
        return detalle;
    }

    /**
     * El auditor previamente asignado se resuelve por el historial y no por la solicitud: tras un
     * rechazo o un vencimiento el campo {@code auditor} queda nulo, asi que sin mirar el historial
     * un auditor perderia el acceso al detalle de la auditoria en la que participo justo despues de
     * responderla, que es cuando quiere confirmar que su respuesta quedo registrada.
     */
    private boolean puedeConsultar(SolicitudAuditoria solicitud, Usuario usuario) {
        if (usuario.getRol() == Rol.ADMINISTRADOR_PLATAFORMA) {
            return true;
        }

        UUID empresaSolicitud = solicitud.getEmpresa() == null ? null : solicitud.getEmpresa().getId();
        UUID empresaUsuario = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId();
        if (empresaSolicitud != null && empresaSolicitud.equals(empresaUsuario)) {
            return true;
        }

        if (solicitud.getAuditor() != null && solicitud.getAuditor().getId().equals(usuario.getId())) {
            return true;
        }

        return transicionEstadoAuditoriaRepository
                .idsAuditoresConHistorial(solicitud.getId())
                .contains(usuario.getId());
    }
}
