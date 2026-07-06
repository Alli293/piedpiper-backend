package com.piedpiper.carbonhub.auth.models.dtos;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroEmpresaRequestDTO {

    @NotBlank(message = "El token de Google es obligatorio.")
    private String idToken;

    @NotBlank(message = "Ingresa el nombre de la empresa.")
    @Size(min = 2, max = 150, message = "Ingresa el nombre de la empresa.")
    private String nombreEmpresa;

    @NotNull(message = "Selecciona una opción válida")
    private SectorIndustrial sectorIndustrial;

    @NotBlank(message = "Selecciona una opción válida")
    @Size(min = 2, max = 2, message = "Selecciona una opción válida")
    private String pais;

    @Min(value = 1, message = "Ingresa un número de empleados mayor que 0.")
    private int cantidadEmpleados;

    @NotBlank(message = "Ingresa un correo electrónico válido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 254, message = "Ingresa un correo electrónico válido")
    private String correoCorporativo;

    @AssertTrue(message = "Debes aceptar los Términos y Condiciones y la Política de Privacidad.")
    private boolean aceptaTerminos;
}
