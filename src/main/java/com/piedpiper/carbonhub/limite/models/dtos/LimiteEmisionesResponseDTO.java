package com.piedpiper.carbonhub.limite.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LimiteEmisionesResponseDTO(
        Long id,
        UUID empresaId,
        Integer anio,
        BigDecimal limiteMt,
        String justificacion,
        String mensaje,
        LocalDateTime actualizadoEn
) {
}
