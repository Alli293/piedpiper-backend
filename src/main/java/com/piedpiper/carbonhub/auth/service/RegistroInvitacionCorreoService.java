package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.mappers.UsuarioAuthMapper;
import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionCorreoRequestDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroInvitacionCorreoService {

    private static final Logger log = LoggerFactory.getLogger(RegistroInvitacionCorreoService.class);

    private final InvitacionService invitacionService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioAuthMapper usuarioAuthMapper;

    public RegistroInvitacionCorreoService(InvitacionService invitacionService,
                                           UsuarioRepository usuarioRepository,
                                           PasswordEncoder passwordEncoder,
                                           JwtService jwtService,
                                           UsuarioAuthMapper usuarioAuthMapper) {
        this.invitacionService = invitacionService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioAuthMapper = usuarioAuthMapper;
    }

    @Transactional
    public AuthResponseDTO registrar(RegistroInvitacionCorreoRequestDTO request) {
        Invitacion invitacion = invitacionService.validarParaAceptar(request.getTokenInvitacion());

        if (usuarioRepository.existsByEmailIgnoreCase(invitacion.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        }

        Usuario usuario = Usuario.builder()
                .email(invitacion.getEmail())
                .nombre(Usuario.recortarNombre(request.getNombre()))
                .apellidos(Usuario.recortarNombre(request.getApellidos()))
                .passwordHash(passwordEncoder.encode(request.getContrasena()))
                .rol(Rol.USUARIO_GENERAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .empresa(invitacion.getEmpresa())
                .fechaRegistro(Instant.now())
                .build();

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
            invitacionService.marcarAceptada(invitacion);

            String token = jwtService.generar(usuario);
            return usuarioAuthMapper.toAuthResponse(usuario, token, RedirectResolver.paraUsuario(usuario));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta en CarbonHub. ¿Deseas iniciar sesión?");
        } catch (Exception e) {
            log.error("Error inesperado al registrar usuario por invitacion", e);
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar tu cuenta. Por favor, intenta nuevamente.");
        }
    }
}
