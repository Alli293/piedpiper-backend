package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// La relacion Usuario.empresa (@ManyToOne) se reagrego como parte de este PR (PP-25/PP-26),
// coordinado con el equipo, ya que el merge de PP-18 la habia dejado fuera.
@Service
public class RegistroEmpresaCorreoService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificacionService emailVerificacionService;

    public RegistroEmpresaCorreoService(UsuarioRepository usuarioRepository,
                                        EmpresaRepository empresaRepository,
                                        PasswordEncoder passwordEncoder,
                                        EmailVerificacionService emailVerificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificacionService = emailVerificacionService;
    }

    @Transactional
    public RegistroPendienteResponseDTO registrar(RegistroEmpresaCorreoRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmailAdmin())) {
            throw ApiException.cuentaDuplicada(
                    "Este correo ya tiene una cuenta registrada. ¿Deseas iniciar sesión?");
        }
        if (empresaRepository.existsByCorreoCorporativo(request.getCorreoCorporativo())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con este correo corporativo.");
        }
        if (empresaRepository.existsByCedulaJuridica(request.getCedulaJuridica())) {
            throw ApiException.cuentaDuplicada(
                    "Ya existe una empresa registrada con esta cédula jurídica.");
        }

        String slug = generarSlugUnico(request.getNombreEmpresa());

        Empresa empresa = Empresa.builder()
                .nombreEmpresa(request.getNombreEmpresa())
                .cedulaJuridica(request.getCedulaJuridica())
                .sectorIndustrial(request.getSectorIndustrial())
                .pais(request.getPais())
                .cantidadEmpleados(request.getCantidadEmpleados())
                .correoCorporativo(request.getCorreoCorporativo())
                .slug(slug)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        Usuario admin;
        try {
            Empresa empresaGuardada = empresaRepository.saveAndFlush(empresa);

            admin = Usuario.builder()
                    .nombre(Usuario.recortarNombre(request.getNombreAdmin()))
                    .apellidos(Usuario.recortarNombre(request.getApellidosAdmin()))
                    .email(request.getEmailAdmin())
                    .passwordHash(passwordEncoder.encode(request.getContrasena()))
                    .rol(Rol.ADMINISTRADOR_EMPRESA)
                    .metodoAuth(MetodoAuth.CORREO)
                    .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                    .fechaRegistro(Instant.now())
                    .empresa(empresaGuardada)
                    .build();
            admin = usuarioRepository.saveAndFlush(admin);
        } catch (Exception e) {
            throw ApiException.errorInterno(
                    "Ocurrió un error al registrar la empresa. Por favor, intenta nuevamente.");
        }

        emailVerificacionService.enviarCorreoVerificacion(admin.getNombre(), admin.getEmail());

        return new RegistroPendienteResponseDTO(
                "Te enviamos un correo de verificación a tu bandeja de entrada.",
                admin.getEmail());
    }

    private String generarSlugUnico(String nombreEmpresa) {
        String base = Empresa.generarSlug(nombreEmpresa);
        String slug = base;
        int sufijo = 2;
        while (empresaRepository.existsBySlug(slug)) {
            slug = base + "-" + sufijo;
            sufijo++;
        }
        return slug;
    }
}
