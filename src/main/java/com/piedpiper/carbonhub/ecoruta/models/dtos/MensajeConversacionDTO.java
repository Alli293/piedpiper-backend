package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un turno del historial conversacional del chat de refinamiento (PP-88). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeConversacionDTO {

    /** Solo estos dos roles participan de la conversación de refinamiento; nada más es válido. */
    @Pattern(regexp = "USUARIO|ASISTENTE", message = "El rol del mensaje no es válido.")
    private String rol;

    /**
     * El backend nunca persiste el historial (vive del lado del cliente hasta que exista PP-89),
     * así que este mensaje pudo haber sido inventado por el cliente antes de llegar acá. El tope
     * es un piso barato de defensa, mismo límite que {@code mensajeUsuario} en
     * {@link RefinamientoItinerarioRequestDTO} — no es una validación de negocio real, solo evita
     * que un historial manipulado infle el prompt sin límite.
     */
    @Size(max = 1000, message = "El contenido del mensaje no puede superar 1.000 caracteres.")
    private String contenido;
}
