package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefinamientoItinerarioRequestDTO {

    @NotBlank(message = "El mensaje no puede estar vacío.")
    @Size(max = 1000, message = "El mensaje no puede superar 1.000 caracteres.")
    private String mensajeUsuario;

    /** Nulo en el primer mensaje de una sesión de chat, cuando todavía no hay historial previo. */
    @Valid
    private ConversacionContextoDTO contextoConversacional;
}
