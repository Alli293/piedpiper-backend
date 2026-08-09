package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MetricasAuditor {

    private final BigDecimal calificacionPromedio;
    private final Integer totalResenas;
    private final Integer auditoriasCompletadas;
    private final BigDecimal tiempoPromedioRespuestaDias;
}
