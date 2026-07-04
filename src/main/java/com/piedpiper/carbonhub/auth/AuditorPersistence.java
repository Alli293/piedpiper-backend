package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auditor.EstadoSolicitud;
import com.piedpiper.carbonhub.auditor.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditor.SolicitudValidacion;
import com.piedpiper.carbonhub.auditor.SolicitudValidacionRepository;
import com.piedpiper.carbonhub.auth.dto.RegistroAuditorRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.user.EstadoUsuario;
import com.piedpiper.carbonhub.user.MetodoAuth;
import com.piedpiper.carbonhub.user.Rol;
import com.piedpiper.carbonhub.user.Usuario;
import com.piedpiper.carbonhub.user.UsuarioRepository;
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
    public void persistir(RegistroAuditorRequest request, GoogleClaims claims,
                          String certificadoPath, String identificacionPath) {
        Usuario auditor = Usuario.builder()
                .googleSub(claims.sub())
                .email(claims.email())
                .nombre(claims.name() != null ? claims.name() : request.nombreCompleto())
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build();
        auditor = usuarioRepository.save(auditor);

        PerfilAuditor perfil = PerfilAuditor.builder()
                .usuario(auditor)
                .nombreCompleto(request.nombreCompleto())
                .numeroCertificacion(request.numeroCertificacion())
                .entidadCertificadora(request.entidadCertificadora())
                .fechaVigenciaCert(request.fechaVigenciaCert())
                .aniosExperiencia(request.aniosExperiencia())
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
