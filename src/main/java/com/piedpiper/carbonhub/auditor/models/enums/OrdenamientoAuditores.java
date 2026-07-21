package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.exceptions.ApiException;

import org.springframework.data.domain.Sort;

public enum OrdenamientoAuditores {
    CALIFICACION(Sort.by(Sort.Order.desc("calificacionPromedio").nullsLast())),
    AUDITORIAS_COMPLETADAS(Sort.by(Sort.Order.desc("auditoriasCompletadas"))),
    TIEMPO_RESPUESTA(Sort.by(Sort.Order.asc("tiempoRespuestaHoras").nullsLast()));

    private final Sort sort;

    OrdenamientoAuditores(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    public static OrdenamientoAuditores desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CALIFICACION;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.ordenamientoAuditoresInvalido();
        }
    }
}
