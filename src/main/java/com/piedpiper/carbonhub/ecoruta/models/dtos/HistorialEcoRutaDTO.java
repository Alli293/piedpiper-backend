package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Historial previo de EcoRuta del usuario autenticado. Se construye internamente a partir de
 * {@code ItinerarioRepository} — nunca se acepta este DTO desde el cliente, para garantizar que
 * siempre corresponda al usuario de la sesión activa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEcoRutaDTO {

    private int totalItinerariosGenerados;
    private List<String> provinciasVisitadas;
}
