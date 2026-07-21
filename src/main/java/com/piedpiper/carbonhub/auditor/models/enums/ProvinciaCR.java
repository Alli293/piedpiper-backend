package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.exceptions.ApiException;

public enum ProvinciaCR {
    SAN_JOSE("San José"),
    ALAJUELA("Alajuela"),
    CARTAGO("Cartago"),
    HEREDIA("Heredia"),
    GUANACASTE("Guanacaste"),
    PUNTARENAS("Puntarenas"),
    LIMON("Limón");

    private final String etiqueta;

    ProvinciaCR(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static ProvinciaCR desde(String valor) {
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ApiException.zonaAuditorInvalida();
        }
    }
}
