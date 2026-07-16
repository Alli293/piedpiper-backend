package com.piedpiper.carbonhub.emision.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UnidadDistancia {
    KM("km"),
    MI("mi");

    private final String value;

    UnidadDistancia(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UnidadDistancia fromValue(String value) {
        for (UnidadDistancia unidad : values()) {
            if (unidad.value.equalsIgnoreCase(value)) {
                return unidad;
            }
        }
        throw new IllegalArgumentException("Seleccione una unidad válida.");
    }
}
