package com.piedpiper.carbonhub.emision.models.dtos;

import java.math.BigDecimal;

public record ReporteHuellaComparacionDTO(
        BigDecimal acumuladoT,
        BigDecimal limiteT,
        BigDecimal porcentajeConsumido,
        String estado) {

    public boolean tieneLimite() {
        return limiteT != null;
    }
}
