package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapper;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Listados de solicitudes de auditoria, uno por cada rol que las mira.
 *
 * <p>Son dos consultas distintas y no una con filtros porque la pertenencia se resuelve distinto:
 * la empresa ve las suyas por {@code empresa_id}, y el auditor ve las que tiene asignadas. Ninguno
 * de los dos recibe un parametro para elegir de quien listar; sale del usuario autenticado, asi que
 * no hay forma de pedir el listado de otro.</p>
 */
@Service
public class SolicitudAuditoriaListadoService {

    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudAuditoriaMapper solicitudAuditoriaMapper;

    public SolicitudAuditoriaListadoService(SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                            UsuarioRepository usuarioRepository,
                                            SolicitudAuditoriaMapper solicitudAuditoriaMapper) {
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.solicitudAuditoriaMapper = solicitudAuditoriaMapper;
    }

    @Transactional(readOnly = true)
    public List<SolicitudAuditoriaResumenResponseDTO> listarDeMiEmpresa(UUID usuarioId) {
        Usuario usuario = usuarioDe(usuarioId);
        if (usuario.getEmpresa() == null || usuario.getEmpresa().getId() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        return solicitudAuditoriaMapper.toResumenDtos(
                solicitudAuditoriaRepository.listarPorEmpresa(usuario.getEmpresa().getId()));
    }

    @Transactional(readOnly = true)
    public List<SolicitudAuditoriaResumenResponseDTO> listarAsignadasA(UUID usuarioId) {
        return solicitudAuditoriaMapper.toResumenDtos(
                solicitudAuditoriaRepository.listarAsignadasA(usuarioId));
    }

    private Usuario usuarioDe(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.errorInterno("No se pudo identificar al usuario autenticado."));
    }
}
