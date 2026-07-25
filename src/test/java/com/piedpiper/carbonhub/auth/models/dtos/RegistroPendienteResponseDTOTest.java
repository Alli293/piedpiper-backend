package com.piedpiper.carbonhub.auth.models.dtos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroPendienteResponseDTOTest {

    @Test
    void constructorCompleto_exponeValoresPorGetters() {
        RegistroPendienteResponseDTO dto = new RegistroPendienteResponseDTO(
                "Te enviamos un correo de verificación a tu bandeja de entrada.",
                "ana.perez@example.com");

        assertThat(dto.getMensaje())
                .isEqualTo("Te enviamos un correo de verificación a tu bandeja de entrada.");
        assertThat(dto.getEmail()).isEqualTo("ana.perez@example.com");
    }

    @Test
    void constructorVacioConSetters_exponeValoresPorGetters() {
        RegistroPendienteResponseDTO dto = new RegistroPendienteResponseDTO();
        dto.setMensaje("Te enviamos un correo de verificación a tu bandeja de entrada.");
        dto.setEmail("ana.perez@example.com");

        assertThat(dto.getMensaje())
                .isEqualTo("Te enviamos un correo de verificación a tu bandeja de entrada.");
        assertThat(dto.getEmail()).isEqualTo("ana.perez@example.com");
    }
}
