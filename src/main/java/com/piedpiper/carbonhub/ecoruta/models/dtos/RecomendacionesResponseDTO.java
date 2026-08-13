package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta de {@code GET /ecoruta/itinerarios/{id}/recomendaciones} (PP-93): recomendaciones
 * ordenadas por impacto ambiental esperado (mayor a menor). {@code mensaje} solo se completa
 * cuando no hay recomendaciones que ofrecer, nunca junto a una lista no vacía. El texto varía
 * según la causa: "excelente desempeño" solo si la clasificación del itinerario es EXCELENTE;
 * un texto neutro en cualquier otro caso sin actividades mejorables (ver
 * {@link com.piedpiper.carbonhub.ecoruta.service.RecomendacionAmbientalService}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionesResponseDTO {

    private List<RecomendacionAmbientalDTO> recomendaciones;
    private String mensaje;
}
