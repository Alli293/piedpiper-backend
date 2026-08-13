package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    /**
     * Momento en que el usuario escribio su contrasena, en segundos. Sobrevive a las renovaciones
     * sin cambiar, que es lo que permite medir cuanto lleva abierta la sesion de verdad: el
     * {@code iat} se reinicia con cada token nuevo y no sirve para eso.
     */
    public static final String CLAIM_INICIO_SESION = "authTime";

    private final SecretKey key;
    private final long expirationMs;
    private final long sesionMaximaMs;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secret,
            @Value("${security.jwt.expiration-time}") long expirationMs,
            @Value("${security.jwt.max-session-time:43200000}") long sesionMaximaMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.sesionMaximaMs = sesionMaximaMs;
    }

    /** Token de inicio de sesion: arranca el reloj de la sesion. */
    public String generar(Usuario usuario) {
        return construir(usuario, System.currentTimeMillis() / 1000);
    }

    /** Token de renovacion: arrastra el inicio original para no reiniciar el reloj. */
    public String renovar(Usuario usuario, long inicioSesionSegundos) {
        return construir(usuario, inicioSesionSegundos);
    }

    /**
     * Cuando arranco la sesion segun el token, o vacio si no lo dice.
     *
     * <p>Los tokens emitidos antes de que existiera el claim no lo traen. Se devuelven vacios a
     * proposito: {@link #puedeRenovarse} los trata como no renovables, asi que caducan solos al
     * cumplir su hora en vez de forzar un cierre de sesion masivo en el momento del despliegue.</p>
     */
    public Optional<Long> inicioSesionDe(Claims claims) {
        Object valor = claims.get(CLAIM_INICIO_SESION);
        return valor instanceof Number numero ? Optional.of(numero.longValue()) : Optional.empty();
    }

    /**
     * Una sesion no puede renovarse para siempre. Sin este tope, como cada peticion devuelve un
     * token nuevo, basta con usar la aplicacion una vez por hora para no cerrar sesion nunca: un
     * token robado seguiria sirviendo indefinidamente.
     */
    public boolean puedeRenovarse(Claims claims) {
        return inicioSesionDe(claims)
                .map(inicio -> System.currentTimeMillis() - inicio * 1000 < sesionMaximaMs)
                .orElse(false);
    }

    private String construir(Usuario usuario, long inicioSesionSegundos) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(usuario.getId().toString())
                .addClaims(Map.of(
                        "email", usuario.getEmail(),
                        "rol", usuario.getRol().name(),
                        "estado", usuario.getEstado().name(),
                        "configuracionCompleta", usuario.isConfiguracionCompleta(),
                        CLAIM_INICIO_SESION, inicioSesionSegundos))
                .setIssuedAt(ahora)
                .setExpiration(expira)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parsear(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
