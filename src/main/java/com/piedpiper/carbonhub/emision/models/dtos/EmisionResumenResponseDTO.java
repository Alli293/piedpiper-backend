package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmisionResumenResponseDTO {

    private Integer anio;
    private Integer mes;
    private BigDecimal totalKg;
    private BigDecimal totalT;
    private List<ResumenCategoriaDTO> categorias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenCategoriaDTO {
        private CategoriaEmision categoria;
        private BigDecimal totalKg;
        private BigDecimal porcentaje;
    }
}
