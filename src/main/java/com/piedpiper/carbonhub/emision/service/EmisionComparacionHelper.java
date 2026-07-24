package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.common.HuellasCarbono;
import com.piedpiper.carbonhub.exceptions.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class EmisionComparacionHelper {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal UMBRAL_CERCA = new BigDecimal("80.0");
    private static final BigDecimal UMBRAL_SUPERADO = new BigDecimal("100.0");

    private EmisionComparacionHelper() {
    }

    static BigDecimal toneladasDesdeKg(BigDecimal kg) {
        return HuellasCarbono.toneladasDesdeKg(kg);
    }

    static BigDecimal porcentajeConsumido(BigDecimal acumuladoT, BigDecimal limiteT) {
        if (limiteT.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.valorNoSoportado("El límite anual debe ser mayor que 0.");
        }
        return acumuladoT.multiply(CIEN).divide(limiteT, 1, RoundingMode.HALF_UP);
    }

    static String estado(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_SUPERADO) > 0) {
            return "superado";
        }
        if (porcentaje.compareTo(UMBRAL_SUPERADO) == 0) {
            return "alcanzado";
        }
        if (porcentaje.compareTo(UMBRAL_CERCA) >= 0) {
            return "cerca";
        }
        return "dentro";
    }
}
