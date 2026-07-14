package com.piedpiper.carbonhub.user.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum Idioma {
    ESPANOL,
    INGLES;

    public static final Idioma POR_DEFECTO = ESPANOL;

    public static Optional<Idioma> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(values())
                .filter(idioma -> idioma.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
