package com.piedpiper.carbonhub.emision.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionMensualDTO {

    private int anio;
    private List<PuntoMensual> serie;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoMensual {
        private int mes;
        private BigDecimal totalCarbonKg;
    }
}
