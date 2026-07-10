package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroEmpresaService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public RegistroEmpresaService(GoogleTokenVerifier googleTokenVerifier,
                                  UsuarioRepository usuarioRepository,
                                  JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroEmpresaRequestDTO request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());

        if (!claims.isEmailVerified()) {
            throw ApiException.correoNoVerificado();
        }
        if (usuarioRepository.existsByGoogleSub(claims.getSub())
                || usuarioRepository.existsByEmail(claims.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        }

        Usuario admin = Usuario.builder()
                .googleSub(claims.getSub())
                .email(claims.getEmail())
                .nombre(Usuario.recortarNombre(claims.getGivenName()))
                .apellidos(Usuario.recortarNombre(claims.getFamilyName()))
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build();
        admin = usuarioRepository.save(admin);

        String token = jwtService.generar(admin);
        return new AuthResponseDTO(token, admin.getRol().name(), admin.getEstado().name(),
                RedirectResolver.paraUsuario(admin));
    }
}
