package com.piedpiper.carbonhub.common;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utilidad para resolver valores de texto contra catálogos de enums,
 * de forma tolerante (case-insensitive, con trim).
 */
public final class Catalogos {

    private Catalogos() {
    }

    public static <E extends Enum<E>> Optional<E> desde(Class<E> tipo, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
