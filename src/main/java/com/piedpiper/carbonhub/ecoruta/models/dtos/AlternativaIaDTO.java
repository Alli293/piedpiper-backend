package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlternativaIaDTO {

    private String nombre;
    private String descripcion;
    private BigDecimal costoAproximado;
    private String moneda;
    private String establecimientoRecomendado;
    private Integer puntuacionAmbientalEstimada;
}
