package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntoHuellaDTO {

    private String periodo;
    private BigDecimal huellaT;
    private BigDecimal variacionPorcentual;
}
