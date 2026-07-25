package com.piedpiper.carbonhub.ima.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImaResponseDTO {

    private BigDecimal cobertura;
    private BigDecimal puntajeIntensidadSectorial;
    private BigDecimal consistencia;
    private BigDecimal ima;
    private boolean parcial;
    private String motivoParcial;
    private BigDecimal intensidad;
    private Instant calculatedAt;
    private String interpretacion;
    private String siguientePaso;
}
