package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.NotBlank;
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

    /**
     * Solo estos dos roles participan de la conversación de refinamiento; nada más es válido.
     * {@code @NotBlank} además de {@code @Pattern}: sin él, {@code rol == null} pasaba la
     * validación (Bean Validation no aplica {@code @Pattern} a un valor nulo) y llegaba tal cual
     * al prompt como {@code "null: <contenido>"} (señalado en revisión).
     */
    @NotBlank(message = "El rol del mensaje es obligatorio.")
    @Pattern(regexp = "USUARIO|ASISTENTE", message = "El rol del mensaje no es válido.")
    private String rol;

    /**
     * El backend nunca persiste el historial (vive del lado del cliente hasta que exista
     * persistencia entre sesiones), así que este mensaje pudo haber sido inventado por el cliente
     * antes de llegar acá. El tope es un piso barato de defensa, mismo límite que
     * {@code mensajeUsuario} en {@link RefinamientoItinerarioRequestDTO} — no es una validación de
     * negocio real, solo evita que un historial manipulado infle el prompt sin límite.
     *
     * <p>{@code @NotBlank} por la misma razón que {@code rol}: sin él, {@code contenido == null}
     * pasaba {@code @Size} (que tampoco rechaza nulos) y producía un turno vacío/ambiguo en el
     * prompt.
     */
    @NotBlank(message = "El contenido del mensaje es obligatorio.")
    @Size(max = 1000, message = "El contenido del mensaje no puede superar 1.000 caracteres.")
    private String contenido;
}
