package com.piedpiper.carbonhub.validacion.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum DecisionSolicitud {
    APROBADO,
    RECHAZADO;

    public static Optional<DecisionSolicitud> desde(String valor) {
        return Catalogos.desde(DecisionSolicitud.class, valor);
    }
}
