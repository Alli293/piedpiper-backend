package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Una alerta de vencimiento para el panel "Alertas activas" del dashboard
 * (PP-76). Se identifica por la certificacion, no por una fila de la tabla
 * {@code alertas} — ver {@code DashboardAlertasService} para el porque.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaVencimientoResponseDTO {
    /**
     * Id de la certificacion asociada. Deliberadamente no se duplica aqui
     * {@code codigoVerificacion} ni otros campos propios de la certificacion
     * (ver {@code CertificacionResumenResponseDTO}): el cliente ya tiene
     * este id y puede cruzarlo con {@code GET /api/certificaciones}.
     */
    private UUID idCertificacion;
    private String nombre;
    private LocalDate fechaVencimiento;
    private long diasRestantes;

    /** 'vencida' | '7_dias' | '30_dias' | '90_dias' (mismos codigos que CertificacionVencimientoDTO, PP-77). */
    private String urgencia;
}
