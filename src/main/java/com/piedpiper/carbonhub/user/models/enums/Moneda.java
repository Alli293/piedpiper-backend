package com.piedpiper.carbonhub.user.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum Moneda {
    CRC,
    USD;

    public static final Moneda POR_DEFECTO = CRC;

    public static Optional<Moneda> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(values())
                .filter(moneda -> moneda.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
