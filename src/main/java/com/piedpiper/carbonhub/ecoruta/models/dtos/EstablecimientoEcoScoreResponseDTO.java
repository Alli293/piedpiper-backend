package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Desglose del EcoScore por establecimiento (PP-91), expuesto junto al itinerario para que el
 * usuario pueda consultar el detalle del cálculo. {@code empresaId} es {@code null} cuando el
 * establecimiento no matcheó con ninguna empresa registrada (ver
 * {@code EcoRutaItinerarioService.extraerEstablecimientosRankeados}); cuando sí lo tiene, el
 * frontend lo usa para pedir el banner de origen del emprendedor a
 * {@code GET /establecimientos/{empresaId}/banner} (PP-95).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientoEcoScoreResponseDTO {

    private String nombreEstablecimiento;
    private UUID empresaId;
    private PuntuacionAmbientalResponseDTO puntuacionAmbiental;
}
