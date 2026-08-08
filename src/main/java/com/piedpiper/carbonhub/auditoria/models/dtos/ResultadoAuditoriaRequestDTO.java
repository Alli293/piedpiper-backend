package com.piedpiper.carbonhub.auditoria.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado final que emite el auditor despues de cargar el reporte.
 *
 * <p>{@code resultado} llega como texto para devolver 422 con un mensaje de dominio cuando el
 * valor no sea reconocido, igual que {@link DecisionAuditorRequestDTO}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAuditoriaRequestDTO {

    @NotBlank(message = "Indica si la auditoria queda aprobada o con observaciones.")
    private String resultado;
}
