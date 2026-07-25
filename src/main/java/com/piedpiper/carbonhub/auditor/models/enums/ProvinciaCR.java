package com.piedpiper.carbonhub.auditor.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;

import java.util.Optional;

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

    public static Optional<ProvinciaCR> desde(String valor) {
        return Catalogos.desde(ProvinciaCR.class, valor);
    }
}
