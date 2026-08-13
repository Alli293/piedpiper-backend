package com.piedpiper.carbonhub.auditoria.models.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ResultadoAuditoria {

    APROBADA("aprobada"),

    /**
     * Acepta {@code observaciones_pendientes} ademas de {@code observaciones}. El primero es el
     * nombre del estado al que lleva y es el que usa la historia; el segundo lo enviaba ya el
     * frontend antes de PP-49. Reconocer los dos evita que la version desplegada del cliente y la
     * del servidor tengan que subir juntas.
     */
    OBSERVACIONES("observaciones", "observaciones_pendientes");

    private final String codigo;
    private final List<String> alias;

    ResultadoAuditoria(String codigo, String... alias) {
        this.codigo = codigo;
        this.alias = List.of(alias);
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
                .filter(resultado -> resultado.reconoce(normalizado))
                .findFirst();
    }

    private boolean reconoce(String valor) {
        return codigo.equalsIgnoreCase(valor)
                || name().equalsIgnoreCase(valor)
                || alias.stream().anyMatch(otro -> otro.equalsIgnoreCase(valor));
    }
}
