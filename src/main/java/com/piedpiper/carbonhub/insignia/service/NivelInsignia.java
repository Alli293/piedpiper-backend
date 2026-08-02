package com.piedpiper.carbonhub.insignia.service;

import java.util.Optional;

public enum NivelInsignia {
    BRONCE("bronce"),
    PLATA("plata"),
    ORO("oro");

    private final String codigo;

    NivelInsignia(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public Optional<NivelInsignia> anterior() {
        return switch (this) {
            case BRONCE -> Optional.empty();
            case PLATA -> Optional.of(BRONCE);
            case ORO -> Optional.of(PLATA);
        };
    }

    public static Optional<NivelInsignia> desde(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        for (NivelInsignia nivel : values()) {
            if (nivel.codigo.equals(normalizado)) {
                return Optional.of(nivel);
            }
        }
        return Optional.empty();
    }
}
