package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlternativaDTO {

    private String nombre;
    private String descripcion;
    private Integer ecoScore;
    private BigDecimal costoAproximado;
    private String moneda;
    private String establecimientoRecomendado;
    private Integer diferenciaAmbiental;
    private boolean mejorDesempeno;
}
