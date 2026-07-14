package com.piedpiper.carbonhub.user.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum UnidadesMedida {
    METRICO;

    public static final UnidadesMedida POR_DEFECTO = METRICO;

    public static Optional<UnidadesMedida> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(values())
                .filter(unidades -> unidades.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
