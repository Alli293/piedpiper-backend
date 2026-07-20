package com.piedpiper.carbonhub.user.models.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    void normalizarEmail_conMayusculasYEspacios_loNormalizaAMinusculasSinEspacios() {
        Usuario usuario = Usuario.builder().email("  Ana@Example.COM  ").build();

        usuario.normalizarEmail();

        assertThat(usuario.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void normalizarEmail_yaNormalizado_loDejaIgual() {
        Usuario usuario = Usuario.builder().email("ana@example.com").build();

        usuario.normalizarEmail();

        assertThat(usuario.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void normalizarEmail_nulo_noLanzaExcepcion() {
        Usuario usuario = Usuario.builder().email(null).build();

        usuario.normalizarEmail();

        assertThat(usuario.getEmail()).isNull();
    }
}
