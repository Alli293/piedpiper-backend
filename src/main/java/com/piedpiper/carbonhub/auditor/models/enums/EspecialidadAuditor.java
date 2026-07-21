package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.exceptions.ApiException;

public enum EspecialidadAuditor {
    AGROINDUSTRIA("Agroindustria"),
    ENERGIA_RENOVABLE("Energía renovable"),
    LOGISTICA_TRANSPORTE("Logística y transporte"),
    MANUFACTURA("Manufactura"),
    TURISMO_SOSTENIBLE("Turismo sostenible");

    private final String etiqueta;

    EspecialidadAuditor(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static EspecialidadAuditor desde(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.especialidadAuditorInvalida();
        }
    }
}
