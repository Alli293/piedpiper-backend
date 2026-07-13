package com.piedpiper.carbonhub.emision.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CabinClass {
    ECONOMY("economy"),
    PREMIUM("premium");

    private final String value;

    CabinClass(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CabinClass fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ECONOMY;
        }
        for (CabinClass cabinClass : values()) {
            if (cabinClass.value.equalsIgnoreCase(value)) {
                return cabinClass;
            }
        }
        throw new IllegalArgumentException("Seleccione una clase valida.");
    }
}
