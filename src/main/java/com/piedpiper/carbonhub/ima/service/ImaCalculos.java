package com.piedpiper.carbonhub.ima.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ImaCalculos {

    public static final int TOTAL_CATEGORIAS = 4;
    public static final int MESES_VENTANA = 12;
    public static final int UMBRAL_EMPRESAS_SECTOR = 5;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CINCUENTA = BigDecimal.valueOf(50);
    private static final BigDecimal KG_POR_TONELADA = BigDecimal.valueOf(1000);

    private ImaCalculos() {
    }

    public static BigDecimal cobertura(long categoriasPresentes) {
        return BigDecimal.valueOf(categoriasPresentes)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(TOTAL_CATEGORIAS), 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal consistencia(long mesesConDatos) {
        return BigDecimal.valueOf(mesesConDatos)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(MESES_VENTANA), 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal intensidadToneladasPorEmpleado(BigDecimal carbonKg, int cantidadEmpleados) {
        return carbonKg
                .divide(KG_POR_TONELADA, 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(cantidadEmpleados), 6, RoundingMode.HALF_UP);
    }

    public static BigDecimal puntajeIntensidadSectorial(BigDecimal intensidad, BigDecimal intensidadPromedio) {
        if (intensidad.compareTo(BigDecimal.ZERO) == 0) {
            return CIEN;
        }
        BigDecimal puntaje = CINCUENTA
                .multiply(intensidadPromedio)
                .divide(intensidad, 1, RoundingMode.HALF_UP);
        return puntaje.max(BigDecimal.ZERO).min(CIEN);
    }

    public static BigDecimal ima(BigDecimal cobertura, BigDecimal puntajeIntensidad, BigDecimal consistencia) {
        if (puntajeIntensidad != null) {
            return cobertura.add(puntajeIntensidad).add(consistencia)
                    .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
        }
        return cobertura.add(consistencia)
                .divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
    }
}
