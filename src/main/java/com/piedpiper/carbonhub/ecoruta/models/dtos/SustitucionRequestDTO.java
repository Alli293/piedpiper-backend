package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SustitucionRequestDTO {

    @NotBlank(message = "El nombre de la alternativa es obligatorio")
    private String nombre;
    private String descripcion;
    private BigDecimal costoAproximado;
    private String moneda;
    private String establecimientoRecomendado;
    @NotNull(message = "El puntaje ecológico (ecoScore) es obligatorio")
    private Integer ecoScore;
    @NotBlank(message = "La categoría turística es obligatoria")
    private String categoriaTuristica;
    @NotBlank(message = "La provincia es obligatoria")
    private String provincia;
}
