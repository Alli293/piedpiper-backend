package com.piedpiper.carbonhub.auditor.models.dtos;

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
public class ActualizarPerfilAuditorRequestDTO {

    @NotEmpty(message = "Seleccione al menos una especialidad.")
    @Size(max = 8, message = "Puede seleccionar un máximo de 8 especialidades.")
    private List<String> especialidades;

    @NotEmpty(message = "Seleccione al menos una zona de cobertura.")
    @Size(max = 7, message = "Puede seleccionar un máximo de 7 zonas de cobertura.")
    private List<String> zonasCobertura;

    @NotNull(message = "El campo disponible es obligatorio.")
    private Boolean disponible;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
    private String descripcionProfesional;
}
