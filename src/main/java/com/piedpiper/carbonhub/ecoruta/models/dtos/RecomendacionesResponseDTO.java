package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de {@code GET /ecoruta/itinerarios/{id}/recomendaciones} (PP-93): recomendaciones
 * ordenadas por impacto ambiental esperado (mayor a menor). {@code mensaje} solo se completa
 * cuando no hay recomendaciones que ofrecer (itinerario ya optimizado), nunca junto a una lista
 * no vacía.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionesResponseDTO {

    private List<RecomendacionAmbientalDTO> recomendaciones;
    private String mensaje;
}
