package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Respuesta de {@code POST /api/ecoruta/itinerarios/{id}/mensajes} (PP-88). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefinamientoItinerarioResponseDTO {

    private ItinerarioResponseDTO itinerario;
    private String respuestaAsistente;
    private List<MensajeConversacionDTO> historialMensajes;

    /** No nulo cuando el mensaje pedía ver alternativas para una actividad puntual. */
    private UUID actividadParaComparar;
}
