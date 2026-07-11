package com.piedpiper.carbonhub.user.models.enums;

import java.util.Optional;

/**
 * Idiomas de interfaz soportados por la plataforma (PP-31).
 * El valor por defecto al crear una cuenta es {@link #ESPANOL}.
 */
public enum Idioma {
    ESPANOL,
    INGLES;

    public static final Idioma POR_DEFECTO = ESPANOL;

    /**
     * Resuelve un valor almacenado o recibido a un idioma soportado.
     * Devuelve vacío si el valor es nulo o ya no pertenece al catálogo.
     */
    public static Optional<Idioma> desdeValor(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        for (Idioma idioma : values()) {
            if (idioma.name().equalsIgnoreCase(valor.trim())) {
                return Optional.of(idioma);
            }
        }
        return Optional.empty();
    }
}
