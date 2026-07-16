package com.piedpiper.carbonhub.limite.models.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LimiteEmisionesResponseDTO {
    private Long id;
    private UUID empresaId;
    private Integer anio;
    private BigDecimal limiteMt;
    private String justificacion;
    private String mensaje;
    private LocalDateTime actualizadoEn;
    private boolean recienCreada;
}
