package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Contexto de la conversación de refinamiento que el cliente reenvía en cada mensaje. El historial
 * vive únicamente del lado del cliente durante la sesión — el backend nunca lo guarda, solo lo usa
 * para armar el prompt de esta llamada y devuelve la versión actualizada para que el cliente la
 * reenvíe en el próximo mensaje.
 *
 * <p>{@code itinerarioId} y {@code versionItinerario}, si vienen, se validan en
 * {@code EcoRutaItinerarioService#refinar} contra el itinerario cargado (400 si el id no coincide
 * con el de la ruta, 409 si la versión quedó desactualizada) para que un contexto de otro
 * itinerario o de una versión vieja no se use para armar el prompt ni termine pisando cambios
 * más recientes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionContextoDTO {

    private UUID itinerarioId;

    /**
     * Tope generoso (nunca debería tocarse en una sesión real: son ~2 mensajes por turno) que
     * actúa como piso de defensa contra un cliente que reenvíe un historial manipulado o inflado —
     * el backend igual solo usa los últimos turnos al armar el prompt
     * ({@code EcoRutaItinerarioService.MAX_TURNOS_HISTORIAL_EN_PROMPT}), esto es una cota dura
     * independiente de esa poda.
     */
    @Valid
    @Size(max = 200, message = "El historial de la conversación es demasiado largo.")
    private List<MensajeConversacionDTO> historialMensajes;

    private Integer versionItinerario;
}
