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

    private static final long SESION_MAXIMA_MS = 43_200_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRATION_MS, SESION_MAXIMA_MS);

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
        assertThat(claims)
                .containsEntry("email", usuario.getEmail())
                .containsEntry("rol", usuario.getRol().name())
                .containsEntry("estado", usuario.getEstado().name())
                .containsEntry("configuracionCompleta", usuario.isConfiguracionCompleta());
    }

    @Test
    void parsearTokenExpiradoLanzaExcepcion() {
        JwtService servicioExpirado = new JwtService(SECRET, -1_000L, SESION_MAXIMA_MS);
        String tokenExpirado = servicioExpirado.generar(usuario());

        assertThatThrownBy(() -> jwtService.parsear(tokenExpirado))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void elTokenDeInicioDeSesionMarcaCuandoEmpezoLaSesion() {
        Claims claims = jwtService.parsear(jwtService.generar(usuario()));

        assertThat(jwtService.inicioSesionDe(claims)).isPresent();
        assertThat(jwtService.puedeRenovarse(claims)).isTrue();
    }

    /**
     * El punto de todo el mecanismo: renovar no puede reiniciar el reloj de la sesion. Si lo
     * reiniciara, el tope absoluto no llegaria nunca y un token robado seguiria vivo para siempre.
     */
    @Test
    void renovarConservaElInicioDeSesionOriginal() {
        Usuario usuario = usuario();
        Claims original = jwtService.parsear(jwtService.generar(usuario));
        long inicio = jwtService.inicioSesionDe(original).orElseThrow();

        Claims renovado = jwtService.parsear(jwtService.renovar(usuario, inicio));

        assertThat(jwtService.inicioSesionDe(renovado)).contains(inicio);
    }

    @Test
    void unaSesionQueSuperaElTopeYaNoSeRenueva() {
        Usuario usuario = usuario();
        long haceTreceHoras = System.currentTimeMillis() / 1000 - 13 * 3600;

        Claims claims = jwtService.parsear(jwtService.renovar(usuario, haceTreceHoras));

        assertThat(jwtService.puedeRenovarse(claims)).isFalse();
    }

    @Test
    void unaSesionDentroDelTopeSeSigueRenovando() {
        Usuario usuario = usuario();
        long haceOnceHoras = System.currentTimeMillis() / 1000 - 11 * 3600;

        Claims claims = jwtService.parsear(jwtService.renovar(usuario, haceOnceHoras));

        assertThat(jwtService.puedeRenovarse(claims)).isTrue();
    }

    /**
     * Los tokens emitidos antes de que existiera el claim no lo traen. No se renuevan, asi que
     * caducan solos al cumplir su hora, en vez de tumbar la sesion de todo el mundo al desplegar.
     */
    @Test
    void unTokenViejoSinLaMarcaNoSeRenuevaPeroSigueSiendoValido() {
        Claims sinMarca = jwtService.parsear(tokenSinInicioDeSesion(usuario()));

        assertThat(jwtService.inicioSesionDe(sinMarca)).isEmpty();
        assertThat(jwtService.puedeRenovarse(sinMarca)).isFalse();
        assertThat(sinMarca.getSubject()).isNotBlank();
    }

    /** Reproduce el formato anterior al claim, que es lo que tendran los tokens ya emitidos. */
    private String tokenSinInicioDeSesion(Usuario usuario) {
        return io.jsonwebtoken.Jwts.builder()
                .setSubject(usuario.getId().toString())
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }
}
