package com.piedpiper.carbonhub.user.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum Idioma {
    ESPANOL,
    INGLES;

    public static final Idioma POR_DEFECTO = ESPANOL;

    public static Optional<Idioma> desde(String valor) {
        return Catalogos.desde(Idioma.class, valor);
    }
}
