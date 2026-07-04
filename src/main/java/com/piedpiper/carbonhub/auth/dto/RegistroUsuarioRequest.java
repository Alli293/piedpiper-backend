package com.piedpiper.carbonhub.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record RegistroUsuarioRequest(
        @NotBlank(message = "El token de Google es obligatorio.")
        String idToken,

        @AssertTrue(message = "Debes aceptar los Términos y Condiciones y la Política de Privacidad.")
        boolean aceptaTerminos) {
}
