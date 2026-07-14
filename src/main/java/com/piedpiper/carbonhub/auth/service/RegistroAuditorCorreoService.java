package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;
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
public class RegistroAuditorCorreoService {

    private static final Logger log = LoggerFactory.getLogger(RegistroAuditorCorreoService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificacionService emailVerificacionService;

    public RegistroAuditorCorreoService(UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        EmailVerificacionService emailVerificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificacionService = emailVerificacionService;
    }

    @Transactional
    public RegistroPendienteResponseDTO registrar(RegistroAuditorCorreoRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?");
        }

        String token = TokenVerificacionGenerator.generar();
        Instant expiracion = TokenVerificacionGenerator.calcularExpiracion();

        Usuario usuario = Usuario.builder()
                .nombre(Usuario.recortarNombre(request.getNombre()))
                .apellidos(Usuario.recortarNombre(request.getApellidos()))
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getContrasena()))
                .rol(Rol.AUDITOR_CERTIFICADO)
                .metodoAuth(MetodoAuth.CORREO)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .configuracionCompleta(false)
                .fechaRegistro(Instant.now())
                .tokenVerificacionHash(TokenVerificacionGenerator.hash(token))
                .tokenVerificacionExpiracion(expiracion)
                .build();

        try {
            usuario = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una cuenta con este correo. ¿Deseas iniciar sesión?");
        } catch (Exception e) {
            log.error("Error inesperado al registrar auditor por correo", e);
            throw ApiException.errorInterno(
                    "Ocurrió un error al crear tu cuenta. Por favor, intenta nuevamente.");
        }

        emailVerificacionService.enviarCorreoVerificacion(usuario.getNombre(), usuario.getEmail(), token);

        return new RegistroPendienteResponseDTO(
                "Te enviamos un correo de verificación a tu bandeja de entrada.",
                usuario.getEmail());
    }
}
