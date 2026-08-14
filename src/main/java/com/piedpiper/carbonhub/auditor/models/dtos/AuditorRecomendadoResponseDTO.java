package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Una tarjeta de auditor recomendado. La {@code justificacion} es nula cuando la IA no pudo
 * generarla: en ese caso la tarjeta se muestra igual, solo sin ese campo (degradación controlada).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditorRecomendadoResponseDTO {

    private UUID auditorId;
    private String nombre;
    private String fotoPerfil;
    private List<String> especialidades;
    private BigDecimal calificacionPromedio;
    private boolean disponible;

    /**
     * Primitivo a propósito, aunque {@code PerfilAuditor.auditoriasCompletadas} es {@code Integer}
     * nullable: la pantalla muestra un número y "sin datos" ahí significa cero. Quien arme este DTO
     * tiene que normalizar el nulo antes, como hace
     * {@code RecomendacionAuditoresConsultaService.auditoriasCompletadasDe}; asignar el campo de la
     * entidad directo desempaquetaría el nulo y lanzaría {@code NullPointerException}.
     */
    private int auditoriasCompletadas;

    private String justificacion;
}
