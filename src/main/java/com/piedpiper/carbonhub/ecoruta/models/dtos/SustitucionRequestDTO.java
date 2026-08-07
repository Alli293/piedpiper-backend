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

    @NotBlank
    private String nombre;
    private String descripcion;
    private BigDecimal costoAproximado;
    private String moneda;
    private String establecimientoRecomendado;
    @NotNull
    private Integer ecoScore;
}
