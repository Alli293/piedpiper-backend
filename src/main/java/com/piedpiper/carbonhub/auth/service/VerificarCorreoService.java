package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auditor.service.PerfilAuditorService;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class VerificarCorreoService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAuditorService perfilAuditorService;

    public VerificarCorreoService(UsuarioRepository usuarioRepository,
                                  PerfilAuditorService perfilAuditorService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilAuditorService = perfilAuditorService;
    }

    @Transactional
    public MensajeResponseDTO verificar(String token) {
        String tokenHash = TokenVerificacionGenerator.hash(token);
        Usuario usuario = usuarioRepository.findByTokenVerificacionHash(tokenHash)
                .orElseThrow(ApiException::tokenVerificacionInvalido);

        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            return new MensajeResponseDTO("¡Correo verificado! Ya puedes iniciar sesión.");
        }

        if (usuario.getTokenVerificacionExpiracion() == null
                || usuario.getTokenVerificacionExpiracion().isBefore(Instant.now())) {
            throw ApiException.tokenVerificacionExpirado();
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setTokenVerificacionHash(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.saveAndFlush(usuario);
        perfilAuditorService.asegurarPerfil(usuario);

        return new MensajeResponseDTO("¡Correo verificado! Ya puedes iniciar sesión.");
    }
}
