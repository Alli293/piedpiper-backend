package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorRequestDTO;
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
public class RegistroAuditorService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public RegistroAuditorService(GoogleTokenVerifier googleTokenVerifier,
                                  UsuarioRepository usuarioRepository,
                                  JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroAuditorRequestDTO request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());

        if (!claims.isEmailVerified()) {
            throw ApiException.correoNoVerificado();
        }
        if (usuarioRepository.existsByGoogleSub(claims.getSub())
                || usuarioRepository.existsByEmailIgnoreCase(claims.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        }

        Usuario auditor = Usuario.builder()
                .googleSub(claims.getSub())
                .email(claims.getEmail())
                .nombre(Usuario.recortarNombre(claims.getGivenName()))
                .apellidos(Usuario.recortarNombre(claims.getFamilyName()))
                .rol(Rol.AUDITOR_CERTIFICADO)
                // Google ya confirma el correo, así que se salta PENDIENTE_VERIFICACION, pero
                // igual debe pasar por la validación del administrador de plataforma antes de
                // operar como auditor — antes quedaba ACTIVO de inmediato, saltándose por completo
                // la revisión de credenciales que sí aplica al registro por correo.
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .metodoAuth(MetodoAuth.GOOGLE)
                .fechaRegistro(Instant.now())
                .build();
        auditor = usuarioRepository.save(auditor);

        String token = jwtService.generar(auditor);
        return new AuthResponseDTO(token, auditor.getRol().name(), auditor.getEstado().name(),
                RedirectResolver.paraUsuario(auditor));
    }
}
