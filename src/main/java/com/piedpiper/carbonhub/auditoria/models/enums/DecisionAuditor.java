package com.piedpiper.carbonhub.auditoria.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum DecisionAuditor {
    ACEPTADA("aceptada"),
    RECHAZADA("rechazada");

    private final String codigo;

    DecisionAuditor(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Optional<DecisionAuditor> desde(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String normalizado = codigo.trim();
        return Arrays.stream(values())
                .filter(decision -> decision.codigo.equalsIgnoreCase(normalizado)
                        || decision.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
