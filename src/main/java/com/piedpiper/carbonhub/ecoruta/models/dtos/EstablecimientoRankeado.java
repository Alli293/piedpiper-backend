package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientoRankeado {

    private UUID empresaId;
    private String nombreEstablecimiento;
    private BigDecimal puntuacionTuristica;
    private BigDecimal puntuacionAmbiental;
    private BigDecimal puntuacionFinal;
    private PuntuacionAmbientalResponseDTO detalleAmbiental;
}
