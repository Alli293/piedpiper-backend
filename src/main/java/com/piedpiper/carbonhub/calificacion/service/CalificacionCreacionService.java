package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.common.Autenticaciones;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class CalificacionCreacionService {

    private final CalificacionRepository calificacionRepository;
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private final PerfilAuditorRepository perfilAuditorRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalificacionMapper calificacionMapper;

    public CalificacionCreacionService(CalificacionRepository calificacionRepository,
                                       SolicitudAuditoriaRepository solicitudAuditoriaRepository,
                                       PerfilAuditorRepository perfilAuditorRepository,
                                       UsuarioRepository usuarioRepository,
                                       CalificacionMapper calificacionMapper) {
        this.calificacionRepository = calificacionRepository;
        this.solicitudAuditoriaRepository = solicitudAuditoriaRepository;
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.usuarioRepository = usuarioRepository;
        this.calificacionMapper = calificacionMapper;
    }

    @Transactional
    public CalificacionResponseDTO crear(CrearCalificacionRequestDTO request, Authentication authentication) {
        UUID usuarioId = Autenticaciones.usuarioId(authentication);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado("No tiene permiso para calificar esta auditoría."));

        SolicitudAuditoria auditoria = solicitudAuditoriaRepository.findById(request.getAuditoriaId())
                .orElseThrow(() -> ApiException.valorNoSoportado("La auditoría no fue encontrada."));

        verificarPermisoEmpresa(usuario, auditoria);
        verificarEstadoAuditoria(auditoria);
        verificarUnicidad(request.getAuditoriaId(), usuario.getEmpresa().getId());

        Instant ahora = Instant.now();
        Calificacion calificacion = Calificacion.builder()
                .auditoria(auditoria)
                .auditor(auditoria.getAuditor())
                .empresa(usuario.getEmpresa())
                .calificacion(request.getCalificacion())
                .comentario(request.getComentario())
                .nombreCalificador(usuario.getNombre() + " " + usuario.getApellidos())
                .creadoEn(ahora)
                .actualizadoEn(ahora)
                .build();

        Calificacion persistida = calificacionRepository.save(calificacion);

        recalcularPromedio(auditoria.getAuditor().getId());

        return calificacionMapper.toDto(persistida);
    }

    private void verificarPermisoEmpresa(Usuario usuario, SolicitudAuditoria auditoria) {
        if (usuario.getEmpresa() == null
                || !usuario.getEmpresa().getId().equals(auditoria.getEmpresa().getId())) {
            throw ApiException.accesoDenegado("No tiene permiso para calificar esta auditoría.");
        }
    }

    private void verificarEstadoAuditoria(SolicitudAuditoria auditoria) {
        if (auditoria.getEstado() != EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA) {
            throw ApiException.auditoriaNoCalificable();
        }
    }

    private void verificarUnicidad(UUID auditoriaId, UUID empresaId) {
        if (calificacionRepository.existsByAuditoriaIdAndEmpresaId(auditoriaId, empresaId)) {
            throw ApiException.calificacionDuplicada();
        }
    }

    private void recalcularPromedio(UUID auditorId) {
        Double promedio = calificacionRepository.promedioByAuditorId(auditorId).orElse(null);
        if (promedio == null) {
            return;
        }

        BigDecimal promedioRedondeado = BigDecimal.valueOf(promedio)
                .setScale(1, RoundingMode.HALF_UP);

        PerfilAuditor perfil = perfilAuditorRepository.findByAuditorId(auditorId)
                .orElse(null);
        if (perfil != null) {
            perfil.setCalificacionPromedio(promedioRedondeado);
            perfilAuditorRepository.save(perfil);
        }
    }
}
