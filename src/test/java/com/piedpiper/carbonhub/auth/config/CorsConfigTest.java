package com.piedpiper.carbonhub.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {

    @Test
    void aceptaSoloOrigenesExplicitos() {
        assertThatCode(() -> new CorsConfig("https://app.carbonhub.example")
                .corsConfigurationSource()).doesNotThrowAnyException();
    }

    @Test
    void rechazaWildcard() {
        assertThatThrownBy(() -> new CorsConfig("*").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite '*'");
    }

    @Test
    void rechazaListaVacia() {
        assertThatThrownBy(() -> new CorsConfig(" , ").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class);
    }
}
