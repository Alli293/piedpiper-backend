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

    /**
     * Indica si la puntuación es estimada (no verificada con datos reales).
     * True cuando el establecimiento no tiene indicadores ambientales registrados
     * en CarbonHub y se le asigna un score base estimado.
     */
    private boolean estimado;

    public PuntuacionAmbientalResponseDTO(BigDecimal puntuacionTotal, BigDecimal componenteCertificaciones,
                                          BigDecimal componenteIma, BigDecimal componenteBenchmark,
                                          int cantidadCertificacionesActivas) {
        this.puntuacionTotal = puntuacionTotal;
        this.componenteCertificaciones = componenteCertificaciones;
        this.componenteIma = componenteIma;
        this.componenteBenchmark = componenteBenchmark;
        this.cantidadCertificacionesActivas = cantidadCertificacionesActivas;
        this.estimado = false;
    }
}
