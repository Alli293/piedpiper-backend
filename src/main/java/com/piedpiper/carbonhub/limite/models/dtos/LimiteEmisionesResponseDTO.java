package com.piedpiper.carbonhub.limite.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimiteEmisionesResponseDTO(
        Long id,
        Long empresaId,
        Integer anio,
        BigDecimal limiteMt,
        String justificacion,
        String mensaje,
        LocalDateTime actualizadoEn
) {
}
