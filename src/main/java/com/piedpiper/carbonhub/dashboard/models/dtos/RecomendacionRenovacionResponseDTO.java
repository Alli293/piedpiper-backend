package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bloque "Recomendación de renovación" del dashboard de Certificaciones
 * (PP-72). {@code justificacion} y {@code sugerenciaAccion} son {@code
 * null} cuando la IA no estuvo disponible — el front debe mostrar el
 * mensaje de no disponibilidad en ese caso, no un texto vacío.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacionRenovacionResponseDTO {
    private UUID idCertificacion;
    private String nombreCertificacion;
    private LocalDate fechaVencimiento;
    private int diasRestantes;
    private BigDecimal impactoHuellaT;
    private String justificacion;
    private String sugerenciaAccion;
}
