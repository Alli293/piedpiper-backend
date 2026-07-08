package com.piedpiper.carbonhub.auth.models.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroEmpresaRequestDTO {

    @NotBlank(message = "El token de Google es obligatorio.")
    private String idToken;

    @AssertTrue(message = "Debes aceptar los Términos y Condiciones y la Política de Privacidad.")
    private boolean aceptaTerminos;
}
