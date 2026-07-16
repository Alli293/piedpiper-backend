package com.piedpiper.carbonhub.user.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

public enum Moneda {
    CRC,
    USD;

    public static final Moneda POR_DEFECTO = CRC;

    public static Optional<Moneda> desde(String valor) {
        return Catalogos.desde(Moneda.class, valor);
    }
}
