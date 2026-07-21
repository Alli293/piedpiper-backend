package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

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

    public static Optional<EspecialidadAuditor> desde(String valor) {
        return Catalogos.desde(EspecialidadAuditor.class, valor);
    }
}
