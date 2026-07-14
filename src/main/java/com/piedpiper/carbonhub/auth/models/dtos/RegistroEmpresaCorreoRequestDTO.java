package com.piedpiper.carbonhub.auth.models.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroEmpresaCorreoRequestDTO {

    @NotBlank(message = "Ingresa el nombre del administrador.")
    @Size(min = 2, max = 100, message = "Ingresa el nombre del administrador.")
    private String nombreAdmin;

    @NotBlank(message = "Ingresa los apellidos del administrador.")
    @Size(min = 2, max = 100, message = "Ingresa los apellidos del administrador.")
    private String apellidosAdmin;

    @NotBlank(message = "Ingresa un correo electrónico válido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 254, message = "Ingresa un correo electrónico válido")
    private String emailAdmin;

    @NotBlank(message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    @Pattern(regexp = RegistroUsuarioCorreoRequestDTO.PATRON_CONTRASENA,
            message = "La contraseña debe tener al menos 8 caracteres, con una letra y un número.")
    @Size(max = 72, message = "La contraseña no puede exceder 72 caracteres.")
    private String contrasena;

    @NotBlank(message = "Debes confirmar tu contraseña.")
    private String confirmarContrasena;

    @AssertTrue(message = "Debes aceptar los Términos y Condiciones y la Política de Privacidad.")
    private boolean aceptaTerminos;

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isContrasenasCoinciden() {
        return contrasena != null && contrasena.equals(confirmarContrasena);
    }
}
