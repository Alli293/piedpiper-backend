package com.piedpiper.carbonhub.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenVerificacionGeneratorTest {

    @Test
    void generar_nuncaDevuelveNuloNiVacio() {
        String token = TokenVerificacionGenerator.generar();

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generar_dosLlamadasConsecutivasProducenTokensDistintos() {
        String tokenUno = TokenVerificacionGenerator.generar();
        String tokenDos = TokenVerificacionGenerator.generar();

        assertThat(tokenUno).isNotEqualTo(tokenDos);
    }

    @Test
    void generar_esBase64UrlSafeSinCaracteresInvalidosParaUrl() {
        String token = TokenVerificacionGenerator.generar();

        assertThat(token).matches("^[A-Za-z0-9_-]+$");
        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    void calcularExpiracion_estaEntre23h59mY24h01mDespuesDeAhora() {
        Instant antes = Instant.now();

        Instant expiracion = TokenVerificacionGenerator.calcularExpiracion();

        Duration diferencia = Duration.between(antes, expiracion);
        assertThat(diferencia).isGreaterThan(Duration.ofHours(23).plusMinutes(59));
        assertThat(diferencia).isLessThan(Duration.ofHours(24).plusMinutes(1));
    }
}
