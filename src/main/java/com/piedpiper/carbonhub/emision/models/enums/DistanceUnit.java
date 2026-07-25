package com.piedpiper.carbonhub.emision.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DistanceUnit {
    KM("km"),
    MI("mi");

    private final String value;

    DistanceUnit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DistanceUnit fromValue(String value) {
        if (value == null || value.isBlank()) {
            return KM;
        }
        for (DistanceUnit unit : values()) {
            if (unit.value.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Seleccione una unidad válida.");
    }
}
