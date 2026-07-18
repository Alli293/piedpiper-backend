package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionRequestDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroInvitacionService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final InvitacionService invitacionService;

    public RegistroInvitacionService(GoogleTokenVerifier googleTokenVerifier,
                                     UsuarioRepository usuarioRepository,
                                     JwtService jwtService,
                                     InvitacionService invitacionService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.invitacionService = invitacionService;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroInvitacionRequestDTO request) {
        Invitacion invitacion = invitacionService.validarParaAceptar(request.getTokenInvitacion());

        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());

        if (!claims.isEmailVerified()) {
            throw ApiException.correoNoVerificado();
        }
        if (!claims.getEmail().equalsIgnoreCase(invitacion.getEmail())) {
            throw ApiException.invitacionCorreoNoCoincide();
        }
        if (usuarioRepository.existsByGoogleSub(claims.getSub())
                || usuarioRepository.existsByEmailIgnoreCase(claims.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        }

        Usuario usuario = Usuario.builder()
                .googleSub(claims.getSub())
                .email(claims.getEmail())
                .nombre(Usuario.recortarNombre(claims.getGivenName()))
                .apellidos(Usuario.recortarNombre(claims.getFamilyName()))
                .rol(Rol.USUARIO_GENERAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.GOOGLE)
                .empresa(invitacion.getEmpresa())
                .fechaRegistro(Instant.now())
                .build();

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        }

        invitacionService.marcarAceptada(invitacion);

        String token = jwtService.generar(usuario);
        return new AuthResponseDTO(token, usuario.getRol().name(), usuario.getEstado().name(),
                RedirectResolver.paraUsuario(usuario));
    }
}
