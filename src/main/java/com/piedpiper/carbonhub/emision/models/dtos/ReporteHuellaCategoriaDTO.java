package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.math.BigDecimal;

public record ReporteHuellaCategoriaDTO(
        CategoriaEmision categoria,
        BigDecimal carbonKg,
        BigDecimal porcentaje) {
}
