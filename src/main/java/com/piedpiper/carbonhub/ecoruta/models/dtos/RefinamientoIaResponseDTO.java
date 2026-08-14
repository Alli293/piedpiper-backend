package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Shape crudo que se le pide a Gemini para interpretar un mensaje de refinamiento (PP-88). Nunca
 * se expone al frontend. Reutiliza {@link ItinerarioIaResponseDTO} para el itinerario actualizado
 * en vez de duplicar sus {@code DiaIaDTO}/{@code ActividadIaDTO} anidados, así
 * {@code ItinerarioValidador} se reusa sin cambios.
 *
 * <p>Exactamente uno de estos tres casos aplica por respuesta:
 * <ul>
 *   <li>{@code requiereAclaracion=true}: el mensaje es ambiguo o imposible de satisfacer.
 *       {@code itinerarioActualizado} y {@code actividadParaComparar} quedan null.</li>
 *   <li>{@code actividadParaComparar} no nulo: el usuario pide ver otras opciones para una
 *       actividad puntual del itinerario actual (por su {@code id}). {@code itinerarioActualizado}
 *       queda null — el frontend dispara el flujo de comparación de alternativas ya existente
 *       (PP-92) para esa actividad.</li>
 *   <li>{@code itinerarioActualizado} no nulo: el mensaje modifica el itinerario directamente.</li>
 * </ul>
 * En los tres casos {@code respuestaTexto} siempre viene presente, con la explicación en lenguaje
 * natural que se le muestra al usuario en el chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefinamientoIaResponseDTO {

    private boolean requiereAclaracion;
    private String respuestaTexto;
    private UUID actividadParaComparar;
    private ItinerarioIaResponseDTO itinerarioActualizado;
}
