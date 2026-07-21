package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum OrdenamientoAuditores {
    CALIFICACION,
    AUDITORIAS_COMPLETADAS,
    TIEMPO_RESPUESTA;

    public static Optional<OrdenamientoAuditores> desde(String valor) {
        return Catalogos.desde(OrdenamientoAuditores.class, valor);
    }
}
