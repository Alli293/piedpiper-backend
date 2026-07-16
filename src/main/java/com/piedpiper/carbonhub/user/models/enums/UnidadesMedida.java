package com.piedpiper.carbonhub.user.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum UnidadesMedida {
    METRICO;

    public static final UnidadesMedida POR_DEFECTO = METRICO;

    public static Optional<UnidadesMedida> desde(String valor) {
        return Catalogos.desde(UnidadesMedida.class, valor);
    }
}
