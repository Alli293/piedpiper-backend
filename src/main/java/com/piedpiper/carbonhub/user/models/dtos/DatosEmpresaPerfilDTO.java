package com.piedpiper.carbonhub.user.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosEmpresaPerfilDTO {

    @NotBlank(message = "Selecciona una opción válida")
    private String sectorIndustrial;

    @NotBlank(message = "Selecciona una opción válida")
    @Size(max = 100, message = "Selecciona una opción válida")
    private String pais;

    @NotNull(message = "Ingresa un número de empleados mayor que 0.")
    @Positive(message = "Ingresa un número de empleados mayor que 0.")
    private Integer cantidadEmpleados;
}
