package com.piedpiper.carbonhub.empresa.models.dtos;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionInicialEmpresaRequestDTO {

    @NotBlank(message = "Ingresa el nombre legal de la empresa.")
    @Size(min = 2, max = 150, message = "Ingresa el nombre legal de la empresa.")
    private String nombreEmpresa;

    @NotBlank(message = "Formato de cédula jurídica inválido (ej. 3-101-123456).")
    @Pattern(regexp = "^\\d-\\d{3}-\\d{6}$",
            message = "Formato de cédula jurídica inválido (ej. 3-101-123456).")
    private String cedulaJuridica;

    @NotNull(message = "Selecciona una opción válida")
    private SectorIndustrial sectorIndustrial;

    @NotBlank(message = "Selecciona una opción válida")
    private String pais;

    @NotNull(message = "Ingresa un número de empleados mayor que 0.")
    @Positive(message = "Ingresa un número de empleados mayor que 0.")
    private Integer cantidadEmpleados;

    @Size(max = 300, message = "La descripción no puede superar los 300 caracteres.")
    private String descripcion;
}
