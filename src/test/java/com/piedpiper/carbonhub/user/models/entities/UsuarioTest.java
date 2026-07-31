package com.piedpiper.carbonhub.user.models.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    void nombreCompleto_conNombreYApellidos_losUne() {
        Usuario usuario = Usuario.builder().nombre("Ana").apellidos("Mora Vargas").build();

        assertThat(usuario.nombreCompleto()).isEqualTo("Ana Mora Vargas");
    }

    @Test
    void nombreCompleto_soloConNombre_noDejaEspaciosSobrantes() {
        Usuario usuario = Usuario.builder().nombre("Ana").build();

        assertThat(usuario.nombreCompleto()).isEqualTo("Ana");
    }

    @Test
    void nombreCompleto_sinNombreNiApellidos_caeANombreVisible() {
        Usuario usuario = Usuario.builder().nombre("  ").apellidos("").nombreVisible("anamora").build();

        assertThat(usuario.nombreCompleto()).isEqualTo("anamora");
    }

    @Test
    void nombreCompleto_sinNingunNombre_caeALaParteLocalDelCorreoYNuncaEsNulo() {
        Usuario usuario = Usuario.builder().email("ana.mora@acme.cr").build();

        assertThat(usuario.nombreCompleto()).isEqualTo("ana.mora");
    }

    @Test
    void nombreCompleto_sinNingunDato_devuelveVacioYNoNulo() {
        assertThat(Usuario.builder().build().nombreCompleto()).isEmpty();
    }

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
