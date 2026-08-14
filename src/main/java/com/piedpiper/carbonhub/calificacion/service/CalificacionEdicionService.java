package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.EditarCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CalificacionEdicionService {

    private final CalificacionRepository calificacionRepository;
    private final PerfilAuditorRepository perfilAuditorRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalificacionMapper calificacionMapper;

    public CalificacionEdicionService(CalificacionRepository calificacionRepository,
                                      PerfilAuditorRepository perfilAuditorRepository,
                                      UsuarioRepository usuarioRepository,
                                      CalificacionMapper calificacionMapper) {
        this.calificacionRepository = calificacionRepository;
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.usuarioRepository = usuarioRepository;
        this.calificacionMapper = calificacionMapper;
    }

    @Transactional
    public CalificacionResponseDTO editar(UUID calificacionId, EditarCalificacionRequestDTO request,
                                          Authentication authentication) {
        Calificacion calificacion = calificacionRepository.findById(calificacionId)
                .orElseThrow(() -> ApiException.recursoNoEncontrado("La calificación no fue encontrada."));

        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado("No tiene permiso para editar esta calificación."));

        verificarPermisoEmpresa(usuario, calificacion);

        calificacion.setCalificacion(request.getCalificacion());
        calificacion.setComentario(request.getComentario());
        calificacion.setActualizadoEn(Instant.now());

        Calificacion actualizada = calificacionRepository.save(calificacion);

        perfilAuditorRepository.actualizarMetricasCalificacion(calificacion.getAuditor().getId());

        return calificacionMapper.toDto(actualizada);
    }

    private void verificarPermisoEmpresa(Usuario usuario, Calificacion calificacion) {
        if (usuario.getEmpresa() == null) {
            throw ApiException.empresaNoConfigurada();
        }
        if (!usuario.getEmpresa().getId().equals(calificacion.getEmpresa().getId())) {
            throw ApiException.accesoDenegado("No tiene permiso para editar esta calificación.");
        }
    }
}
