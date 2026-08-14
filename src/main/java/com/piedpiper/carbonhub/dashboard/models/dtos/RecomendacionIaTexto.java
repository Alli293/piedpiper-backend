package com.piedpiper.carbonhub.dashboard.models.dtos;

/** Salida estructurada del ChatClient para la recomendación de renovación (PP-72). */
public record RecomendacionIaTexto(
        String justificacion,
        String sugerenciaAccion
) {
}
