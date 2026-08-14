package com.piedpiper.carbonhub.dashboard.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Calendario de vencimientos del dashboard (PP-77). {@code vencimientosPorFecha}
 * solo incluye los días que sí tienen certificaciones venciendo ese día (clave
 * en formato {@code yyyy-MM-dd}); los días sin vencimientos no aparecen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarioVencimientosResponseDTO {
    private String mesVisualizado;
    private Map<String, List<CertificacionVencimientoDTO>> vencimientosPorFecha;
}
