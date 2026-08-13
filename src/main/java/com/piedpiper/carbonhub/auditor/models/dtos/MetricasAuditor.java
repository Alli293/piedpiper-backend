package com.piedpiper.carbonhub.auditor.models.dtos;

import java.math.BigDecimal;

/**
 * Objeto interno de cálculo — no cruza el límite HTTP.
 * Se mantiene en este paquete por conveniencia de imports existentes.
 */
public record MetricasAuditor(
    BigDecimal calificacionPromedio,
    Integer totalResenas,
    Integer auditoriasCompletadas,
    BigDecimal tiempoPromedioRespuestaDias
) {}
