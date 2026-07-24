package com.piedpiper.carbonhub.dashboard.models.enums;

import com.piedpiper.carbonhub.common.Catalogos;
import java.util.Optional;

public enum PeriodoDashboard {
    MES_ACTUAL("mes_actual"),
    TRIMESTRE("trimestre"),
    ANIO("año");

    public static final PeriodoDashboard POR_DEFECTO = MES_ACTUAL;

    private final String valor;

    PeriodoDashboard(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static Optional<PeriodoDashboard> desde(String valor) {
        if (valor != null && ANIO.valor.equalsIgnoreCase(valor.trim())) {
            return Optional.of(ANIO);
        }
        return Catalogos.desde(PeriodoDashboard.class, valor);
    }
}
