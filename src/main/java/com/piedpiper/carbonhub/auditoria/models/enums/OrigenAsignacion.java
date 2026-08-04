package com.piedpiper.carbonhub.auditoria.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum OrigenAsignacion {
    MANUAL,
    RECOMENDACION_IA;

    public static Optional<OrigenAsignacion> desde(String valor) {
        return Catalogos.desde(OrigenAsignacion.class, valor);
    }
}
