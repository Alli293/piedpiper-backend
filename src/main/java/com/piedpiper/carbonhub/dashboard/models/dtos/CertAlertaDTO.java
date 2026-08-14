package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una certificación con alerta activa candidata a la recomendación de
 * renovación (PP-72). {@code impactoHuellaT} se calcula en tiempo real —
 * ver {@code DashboardRecomendacionService} — nunca se persiste.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertAlertaDTO {
    private UUID idCertificacion;
    private String nombreCertificacion;
    private LocalDate fechaVencimiento;
    private int diasRestantes;
    private BigDecimal impactoHuellaT;
}
