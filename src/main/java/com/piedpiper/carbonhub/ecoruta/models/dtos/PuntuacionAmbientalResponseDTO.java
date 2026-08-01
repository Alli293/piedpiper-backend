package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntuacionAmbientalResponseDTO {

    private BigDecimal puntuacionTotal;
    private BigDecimal componenteCertificaciones;
    private BigDecimal componenteIma;
    private BigDecimal componenteBenchmark;
    private Integer cantidadCertificacionesActivas;
}
