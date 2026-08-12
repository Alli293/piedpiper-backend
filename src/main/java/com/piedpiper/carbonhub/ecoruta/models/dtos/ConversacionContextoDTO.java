package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Contexto de la conversación de refinamiento que el cliente reenvía en cada mensaje. El historial
 * vive únicamente del lado del cliente durante la sesión (PP-89, persistencia entre sesiones, no
 * existe todavía) — el backend nunca lo guarda, solo lo usa para armar el prompt de esta llamada
 * y devuelve la versión actualizada para que el cliente la reenvíe en el próximo mensaje.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionContextoDTO {

    private UUID itinerarioId;
    private List<MensajeConversacionDTO> historialMensajes;
    private Integer versionItinerario;
}
