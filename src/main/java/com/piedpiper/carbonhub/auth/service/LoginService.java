package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.LoginRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public AuthResponseDTO login(LoginRequestDTO request) {
        if (request.getMetodo() == MetodoAuth.GOOGLE) {
            return loginGoogle(request);
        }
        return loginCorreo(request);
    }

    private AuthResponseDTO loginGoogle(LoginRequestDTO request) {
        GoogleClaims claims = googleTokenVerifier.verificar(request.getIdToken());
        Usuario usuario = usuarioRepository.findByGoogleSub(claims.getSub())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No encontramos una cuenta con este correo. ¿Deseas registrarte?"));
        verificarHabilitada(usuario);
        return emitir(usuario);
    }

    private AuthResponseDTO loginCorreo(LoginRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa un correo electrónico válido");
        }
        if (request.getContrasena() == null || request.getContrasena().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu contraseña");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);
        if (usuario == null || usuario.getMetodoAuth() != MetodoAuth.CORREO
                || usuario.getPasswordHash() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }

        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos. Intenta de nuevo en 15 minutos.");
        }

        if (!passwordEncoder.matches(request.getContrasena(), usuario.getPasswordHash())) {
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

    private AuthResponseDTO emitir(Usuario usuario) {
        String token = jwtService.generar(usuario);
        return new AuthResponseDTO(token, usuario.getRol().name(), usuario.getEstado().name(),
                RedirectResolver.paraUsuario(usuario));
    }
}
