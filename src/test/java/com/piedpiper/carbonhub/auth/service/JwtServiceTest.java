package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "clave-secreta-de-pruebas-para-firmar-tokens-jwt-123456";
    private static final long EXPIRATION_MS = 1_800_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS);

    private Usuario usuario() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("ana@correo.com")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    @Test
    void generarEstableceExpiracionA30Minutos() {
        Usuario usuario = usuario();

        String token = jwtService.generar(usuario);
        Claims claims = jwtService.parsear(token);

        long duracionMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(duracionMs).isEqualTo(EXPIRATION_MS);
    }

    @Test
    void parsearTokenValidoDevuelveClaims() {
        Usuario usuario = usuario();

        String token = jwtService.generar(usuario);
        Claims claims = jwtService.parsear(token);

        assertThat(claims.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(claims.get("email")).isEqualTo(usuario.getEmail());
        assertThat(claims.get("rol")).isEqualTo(usuario.getRol().name());
    }

    @Test
    void parsearTokenExpiradoLanzaExcepcion() {
        JwtService servicioExpirado = new JwtService(SECRET, -1_000L);
        String tokenExpirado = servicioExpirado.generar(usuario());

        assertThatThrownBy(() -> jwtService.parsear(tokenExpirado))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
