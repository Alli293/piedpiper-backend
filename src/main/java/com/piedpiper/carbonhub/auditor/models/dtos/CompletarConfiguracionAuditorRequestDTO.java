package com.piedpiper.carbonhub.auditor.models.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletarConfiguracionAuditorRequestDTO {

    @NotNull(message = "Indica tus años de experiencia.")
    @Min(value = 0, message = "Los años de experiencia no pueden ser negativos.")
    @Max(value = 60, message = "Ingresa un valor de años de experiencia válido.")
    private Integer aniosExperiencia;

    @NotEmpty(message = "Seleccione al menos una especialidad.")
    @Size(max = 8, message = "Puede seleccionar un máximo de 8 especialidades.")
    private List<String> especialidades;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
    private String descripcionProfesional;

    @Size(max = 300, message = "El sitio web o LinkedIn no puede superar los 300 caracteres.")
    private String sitioWeb;
}
