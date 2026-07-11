package com.piedpiper.carbonhub.user.models.enums;

import java.util.Optional;

/**
 * Monedas soportadas para montos económicos (PP-31).
 * El valor por defecto al crear una cuenta es {@link #CRC}.
 */
public enum Moneda {
    CRC,
    USD;

    public static final Moneda POR_DEFECTO = CRC;

    /**
     * Resuelve un valor almacenado o recibido a una moneda soportada.
     * Devuelve vacío si el valor es nulo o ya no pertenece al catálogo.
     */
    public static Optional<Moneda> desdeValor(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        for (Moneda moneda : values()) {
            if (moneda.name().equalsIgnoreCase(valor.trim())) {
                return Optional.of(moneda);
            }
        }
        return Optional.empty();
    }
}
