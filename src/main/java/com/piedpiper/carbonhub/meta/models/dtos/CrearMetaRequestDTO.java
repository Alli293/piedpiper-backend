package com.piedpiper.carbonhub.meta.models.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de una meta de reducción (PP-78).
 *
 * <p>{@code fechaLimite} se valida en {@code MetaService}, no aquí con
 * {@code @FutureOrPresent}: el criterio de aceptación pide 422 para una
 * fecha límite pasada, y {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * siempre responde 400 en este proyecto (ver {@code GlobalExceptionHandler}).
 * Mismo motivo documentado para {@code PreferenciasUsuarioRequestDTO} en
 * docs/CONVENTIONS.md §7.3.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearMetaRequestDTO {

    @NotBlank(message = "El nombre de la meta debe tener entre 3 y 100 caracteres.")
    @Size(min = 3, max = 100, message = "El nombre de la meta debe tener entre 3 y 100 caracteres.")
    private String nombreMeta;

    @NotNull(message = "Ingresa un valor numérico positivo.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Ingresa un valor numérico positivo.")
    private BigDecimal valorObjetivoHuellaT;

    @NotNull(message = "La fecha límite es obligatoria.")
    private LocalDate fechaLimite;
}
