package com.piedpiper.carbonhub.auth.dto;

import com.piedpiper.carbonhub.user.MetodoAuth;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "El método de inicio de sesión es obligatorio.")
        MetodoAuth metodo,
        String idToken,
        String email,
        String contrasena) {
}
