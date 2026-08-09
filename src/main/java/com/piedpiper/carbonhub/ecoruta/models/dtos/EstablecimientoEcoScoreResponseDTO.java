package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Desglose del EcoScore por establecimiento (PP-91), expuesto junto al itinerario para que el
 * usuario pueda consultar el detalle del cálculo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientoEcoScoreResponseDTO {

    private String nombreEstablecimiento;
    private PuntuacionAmbientalResponseDTO puntuacionAmbiental;
}
