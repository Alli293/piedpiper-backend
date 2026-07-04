package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.LoginRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.user.EstadoUsuario;
import com.piedpiper.carbonhub.user.MetodoAuth;
import com.piedpiper.carbonhub.user.Rol;
import com.piedpiper.carbonhub.user.Usuario;
import com.piedpiper.carbonhub.user.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class LoginService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(GoogleTokenVerifier googleTokenVerifier,
                        UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.metodo() == MetodoAuth.GOOGLE) {
            return loginGoogle(request);
        }
        return loginCorreo(request);
    }

    private AuthResponse loginGoogle(LoginRequest request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.idToken());
        Usuario usuario = usuarioRepository.findByGoogleSub(claims.sub())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No encontramos una cuenta con este correo. ¿Deseas registrarte?"));
        verificarHabilitada(usuario);
        return emitir(usuario);
    }

    private AuthResponse loginCorreo(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa un correo electrónico válido");
        }
        if (request.contrasena() == null || request.contrasena().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu contraseña");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);
        if (usuario == null || usuario.getMetodoAuth() != MetodoAuth.CORREO
                || usuario.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos. Intenta de nuevo en 15 minutos.");
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE_VERIFICACION) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu correo aún no ha sido verificado. Reenviar correo de verificación.");
        }
        verificarHabilitada(usuario);

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        return emitir(usuario);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(Instant.now().plus(BLOQUEO));
        }
        usuarioRepository.save(usuario);
    }

    private void verificarHabilitada(Usuario usuario) {
        if (usuario.getEstado() == EstadoUsuario.RECHAZADO
                || usuario.getEstado() == EstadoUsuario.DESHABILITADO) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Tu cuenta no está habilitada para iniciar sesión.");
        }
    }

    private AuthResponse emitir(Usuario usuario) {
        String token = jwtService.generar(usuario);
        return new AuthResponse(token, usuario.getRol().name(), usuario.getEstado().name(),
                redirect(usuario));
    }

    private String redirect(Usuario usuario) {
        if (usuario.getRol() == Rol.AUDITOR_CERTIFICADO
                && usuario.getEstado() == EstadoUsuario.PENDIENTE_VALIDACION) {
            return "/auditor/validacion-pendiente";
        }
        return switch (usuario.getRol()) {
            case ADMINISTRADOR_EMPRESA -> "/empresa/panel";
            case AUDITOR_CERTIFICADO -> "/auditor/panel";
            case ADMINISTRADOR_PLATAFORMA -> "/admin/panel";
            default -> "/panel";
        };
    }
}
