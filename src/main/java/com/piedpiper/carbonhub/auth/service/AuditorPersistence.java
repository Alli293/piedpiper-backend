package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auditor.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditor.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.auditor.repository.SolicitudValidacionRepository;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AuditorPersistence {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAuditorRepository perfilAuditorRepository;
    private final SolicitudValidacionRepository solicitudValidacionRepository;

    public AuditorPersistence(UsuarioRepository usuarioRepository,
                              PerfilAuditorRepository perfilAuditorRepository,
                              SolicitudValidacionRepository solicitudValidacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilAuditorRepository = perfilAuditorRepository;
        this.solicitudValidacionRepository = solicitudValidacionRepository;
    }

    @Transactional
    public void persistir(RegistroAuditorRequestDTO request, GoogleClaims claims,
                          String certificadoPath, String identificacionPath) {
        Usuario auditor = Usuario.builder()
                .googleSub(claims.getSub())
                .email(claims.getEmail())
                .nombre(Usuario.recortarNombre(
                        claims.getName() != null ? claims.getName() : request.getNombreCompleto()))
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build();
        auditor = usuarioRepository.save(auditor);

        PerfilAuditor perfil = PerfilAuditor.builder()
                .usuario(auditor)
                .nombreCompleto(request.getNombreCompleto())
                .numeroCertificacion(request.getNumeroCertificacion())
                .entidadCertificadora(request.getEntidadCertificadora())
                .fechaVigenciaCert(request.getFechaVigenciaCert())
                .aniosExperiencia(request.getAniosExperiencia())
                .docCertificadoPath(certificadoPath)
                .docIdentificacionPath(identificacionPath)
                .build();
        perfilAuditorRepository.save(perfil);

        SolicitudValidacion solicitud = SolicitudValidacion.builder()
                .auditor(auditor)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(Instant.now())
                .build();
        solicitudValidacionRepository.save(solicitud);
    }
}
