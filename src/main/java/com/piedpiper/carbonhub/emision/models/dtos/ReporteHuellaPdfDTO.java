package com.piedpiper.carbonhub.emision.models.dtos;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record ReporteHuellaPdfDTO(
        String empresa,
        Integer anio,
        Integer mes,
        BigDecimal totalKg,
        BigDecimal totalT,
        List<ReporteHuellaCategoriaDTO> categorias,
        ReporteHuellaComparacionDTO comparacion,
        boolean sinDatos,
        ZonedDateTime generadoEn) {
}
