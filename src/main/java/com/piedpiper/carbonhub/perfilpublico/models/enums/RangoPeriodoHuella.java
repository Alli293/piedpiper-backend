package com.piedpiper.carbonhub.perfilpublico.models.enums;

import java.util.Arrays;
import java.util.Optional;

public enum RangoPeriodoHuella {
    ULTIMO_ANIO("ultimo_anio", 1),
    ULTIMOS_3_ANIOS("ultimos_3_anios", 3),
    HISTORICO("historico", Integer.MAX_VALUE);

    public static final RangoPeriodoHuella POR_DEFECTO = ULTIMOS_3_ANIOS;

    private final String valor;
    private final int cantidadPeriodos;

    RangoPeriodoHuella(String valor, int cantidadPeriodos) {
        this.valor = valor;
        this.cantidadPeriodos = cantidadPeriodos;
    }

    public String getValor() {
        return valor;
    }

    public int getCantidadPeriodos() {
        return cantidadPeriodos;
    }

    public static Optional<RangoPeriodoHuella> desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(rango -> rango.valor.equalsIgnoreCase(valor.trim()))
                .findFirst();
    }
}
