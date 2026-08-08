package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionHuellaPublicaDTO {

    private BigDecimal totalActualTco2e;
    private BigDecimal variacionPorcentual;
    private List<PuntoAnual> serie;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PuntoAnual {
        private int anio;
        private BigDecimal totalTco2e;
    }
}
