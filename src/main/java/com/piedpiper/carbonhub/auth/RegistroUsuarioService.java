package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.RegistroUsuarioRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.user.EstadoUsuario;
import com.piedpiper.carbonhub.user.MetodoAuth;
import com.piedpiper.carbonhub.user.Rol;
import com.piedpiper.carbonhub.user.Usuario;
import com.piedpiper.carbonhub.user.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroUsuarioService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public RegistroUsuarioService(GoogleTokenVerifier googleTokenVerifier,
                                  UsuarioRepository usuarioRepository,
                                  JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegistroUsuarioRequest request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.idToken());

        if (!claims.emailVerified()) {
            throw ApiException.correoNoVerificado();
        }
        if (usuarioRepository.existsByGoogleSub(claims.sub())
                || usuarioRepository.existsByEmail(claims.email())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?");
        }

        Usuario usuario = Usuario.builder()
                .googleSub(claims.sub())
                .email(claims.email())
                .nombre(recortarNombre(claims.name()))
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build();

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?");
        }

        String token = jwtService.generar(usuario);
        return new AuthResponse(token, usuario.getRol().name(), usuario.getEstado().name(),
                "/perfil/configuracion-inicial");
    }

    private String recortarNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.length() > 150 ? nombre.substring(0, 150) : nombre;
    }
}
