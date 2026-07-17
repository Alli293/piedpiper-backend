package com.piedpiper.carbonhub.emision.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionMensualDTO {

    private int anio;
    private List<PuntoMensual> serie;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoMensual {
        private int mes;
        private BigDecimal totalCarbonKg;
    }
}
