package com.piedpiper.carbonhub.user.models.enums;

import java.util.Optional;

/**
 * Sistemas de unidades soportados (PP-31).
 * Para la demo solo se soporta el sistema métrico; las emisiones
 * siguen mostrándose en kg CO₂e y t CO₂e independientemente de esta preferencia.
 */
public enum Unidades {
    METRICO;

    public static final Unidades POR_DEFECTO = METRICO;

    /**
     * Resuelve un valor almacenado o recibido a un sistema de unidades soportado.
     * Devuelve vacío si el valor es nulo o ya no pertenece al catálogo.
     */
    public static Optional<Unidades> desdeValor(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        for (Unidades unidades : values()) {
            if (unidades.name().equalsIgnoreCase(valor.trim())) {
                return Optional.of(unidades);
            }
        }
        return Optional.empty();
    }
}
