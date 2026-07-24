package com.piedpiper.carbonhub.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HuellasCarbono {

    private static final BigDecimal KG_POR_TONELADA = new BigDecimal("1000");

    private HuellasCarbono() {
    }

    public static BigDecimal toneladasDesdeKg(BigDecimal kg) {
        return kg.divide(KG_POR_TONELADA, 4, RoundingMode.HALF_UP);
    }
}
