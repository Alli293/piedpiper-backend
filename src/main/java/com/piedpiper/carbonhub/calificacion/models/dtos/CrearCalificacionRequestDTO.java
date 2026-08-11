package com.piedpiper.carbonhub.calificacion.models.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearCalificacionRequestDTO {

    @NotNull(message = "El campo auditoriaId es requerido.")
    private UUID auditoriaId;

    @NotNull(message = "El campo calificacion es requerido.")
    @Min(value = 1, message = "La calificación debe ser un valor entre 1 y 5.")
    @Max(value = 5, message = "La calificación debe ser un valor entre 1 y 5.")
    private Integer calificacion;

    @Size(max = 500, message = "El comentario no puede superar los 500 caracteres.")
    private String comentario;
}
