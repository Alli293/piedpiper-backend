package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenEmisionesResponseDTO {

    private Integer anio;
    private Integer mes;
    private BigDecimal totalKg;
    private BigDecimal totalT;
    private List<ResumenCategoriaDTO> categorias;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenCategoriaDTO {
        private CategoriaEmision categoria;
        private BigDecimal totalKg;
        private BigDecimal porcentaje;
    }
}
