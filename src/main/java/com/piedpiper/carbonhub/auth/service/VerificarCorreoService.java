package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class VerificarCorreoService {

    private final UsuarioRepository usuarioRepository;

    public VerificarCorreoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MensajeResponseDTO verificar(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(ApiException::tokenVerificacionInvalido);

        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            return new MensajeResponseDTO("¡Correo verificado! Ya puedes iniciar sesión.");
        }

        if (usuario.getTokenVerificacionExpiracion() == null
                || usuario.getTokenVerificacionExpiracion().isBefore(Instant.now())) {
            throw ApiException.tokenVerificacionExpirado();
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpiracion(null);
        usuarioRepository.saveAndFlush(usuario);

        return new MensajeResponseDTO("¡Correo verificado! Ya puedes iniciar sesión.");
    }
}
