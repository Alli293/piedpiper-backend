package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
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
public class RegistroEmpresaCorreoService {

    private static final Logger log = LoggerFactory.getLogger(RegistroEmpresaCorreoService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificacionService emailVerificacionService;

    public RegistroEmpresaCorreoService(UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        EmailVerificacionService emailVerificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificacionService = emailVerificacionService;
    }

    @Transactional
    public RegistroPendienteResponseDTO registrar(RegistroEmpresaCorreoRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmailAdmin())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        }

        String token = TokenVerificacionGenerator.generar();
        Instant expiracion = TokenVerificacionGenerator.calcularExpiracion();

        Usuario admin = Usuario.builder()
                .nombre(Usuario.recortarNombre(request.getNombreAdmin()))
                .apellidos(Usuario.recortarNombre(request.getApellidosAdmin()))
                .email(request.getEmailAdmin())
                .passwordHash(passwordEncoder.encode(request.getContrasena()))
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .metodoAuth(MetodoAuth.CORREO)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .fechaRegistro(Instant.now())
                .tokenVerificacionHash(TokenVerificacionGenerator.hash(token))
                .tokenVerificacionExpiracion(expiracion)
                .build();

        try {
            admin = usuarioRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        } catch (Exception e) {
            log.error("Error inesperado al registrar empresa por correo", e);
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar la empresa. Por favor, intenta nuevamente.");
        }

        emailVerificacionService.enviarCorreoVerificacion(admin.getNombre(), admin.getEmail(), token);

        return new RegistroPendienteResponseDTO(
                "Te enviamos un correo de verificación a tu bandeja de entrada.",
                admin.getEmail());
    }
}
