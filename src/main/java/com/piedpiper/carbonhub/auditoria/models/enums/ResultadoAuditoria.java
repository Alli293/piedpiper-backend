package com.piedpiper.carbonhub.auditoria.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ResultadoAuditoria {
    APROBADA("aprobada"),
    OBSERVACIONES("observaciones");

    private final String codigo;

    ResultadoAuditoria(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Optional<ResultadoAuditoria> desde(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String normalizado = codigo.trim();
        return Arrays.stream(values())
                .filter(resultado -> resultado.codigo.equalsIgnoreCase(normalizado)
                        || resultado.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
