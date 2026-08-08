package com.piedpiper.carbonhub.ecoruta.models.enums;

import java.math.BigDecimal;

public enum ClasificacionAmbiental {
    EXCELENTE,
    BUENA,
    MODERADA,
    MEJORABLE;

    private static final BigDecimal UMBRAL_EXCELENTE = new BigDecimal("80");
    private static final BigDecimal UMBRAL_BUENA = new BigDecimal("60");
    private static final BigDecimal UMBRAL_MODERADA = new BigDecimal("40");

    /**
     * Clasifica un EcoScore (0-100) según los rangos de PP-91:
     * Excelente [80-100], Buena [60-79], Moderada [40-59], Mejorable [0-39].
     */
    public static ClasificacionAmbiental porPuntaje(BigDecimal ecoScore) {
        if (ecoScore.compareTo(UMBRAL_EXCELENTE) >= 0) {
            return EXCELENTE;
        }
        if (ecoScore.compareTo(UMBRAL_BUENA) >= 0) {
            return BUENA;
        }
        if (ecoScore.compareTo(UMBRAL_MODERADA) >= 0) {
            return MODERADA;
        }
        return MEJORABLE;
    }
}
