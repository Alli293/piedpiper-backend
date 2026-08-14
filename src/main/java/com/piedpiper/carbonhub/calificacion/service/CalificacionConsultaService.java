package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CalificacionConsultaService {

    private final CalificacionRepository calificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalificacionMapper calificacionMapper;

    public CalificacionConsultaService(CalificacionRepository calificacionRepository,
                                       UsuarioRepository usuarioRepository,
                                       CalificacionMapper calificacionMapper) {
        this.calificacionRepository = calificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.calificacionMapper = calificacionMapper;
    }

    @Transactional(readOnly = true)
    public CalificacionResponseDTO obtenerPorAuditoria(UUID auditoriaId, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado("No tiene permiso."));

        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }

        UUID empresaId = usuario.getEmpresa().getId();
        return calificacionRepository.findByAuditoriaIdAndEmpresaId(auditoriaId, empresaId)
                .map(calificacionMapper::toDto)
                .orElseThrow(() -> ApiException.recursoNoEncontrado(
                        "No se encontró una calificación para esta auditoría."));
    }
}
