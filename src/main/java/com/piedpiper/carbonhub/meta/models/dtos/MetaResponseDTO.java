package com.piedpiper.carbonhub.meta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una meta de reducción con su progreso calculado (PP-78).
 * {@code huellaActualT} y {@code progresoPorcentaje} se recalculan en cada
 * consulta contra el período seleccionado — no son columnas de la tabla
 * {@code metas}, ver {@code MetaService}.
 *
 * <p>{@code progresoPorcentaje} sigue la fórmula literal del criterio de
 * aceptación de PP-78, {@code (huellaActualT / valorObjetivoHuellaT) × 100},
 * y NO se limita a 100: una empresa cuya huella actual ya superó el
 * objetivo puede mostrar más de 100%. El front decide si recorta la barra
 * visual; el número que se muestra es el real.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaResponseDTO {
    private UUID id;
    private String nombreMeta;
    private BigDecimal valorObjetivoHuellaT;
    private LocalDate fechaLimite;
    private BigDecimal huellaActualT;
    private int progresoPorcentaje;
    private boolean vencida;
    private Instant fechaCreacion;
}
