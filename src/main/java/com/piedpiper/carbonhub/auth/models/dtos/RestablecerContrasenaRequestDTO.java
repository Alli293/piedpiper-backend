package com.piedpiper.carbonhub.auth.models.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerContrasenaRequestDTO {

    @NotBlank(message = "El enlace no es válido.")
    private String token;

    @NotBlank(message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    @Pattern(regexp = RegistroUsuarioCorreoRequestDTO.PATRON_CONTRASENA,
            message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    @Size(max = 72, message = "La contraseña no puede exceder 72 caracteres.")
    private String nuevaContrasena;

    @NotBlank(message = "Debes confirmar tu contraseña.")
    private String confirmarContrasena;

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isContrasenasCoinciden() {
        return nuevaContrasena != null && nuevaContrasena.equals(confirmarContrasena);
    }
}
