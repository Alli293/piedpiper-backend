package com.piedpiper.carbonhub.certificacion.models.enums;

/**
 * Umbrales de vencimiento evaluados por el proceso nocturno de alertas
 * (PP-70). Cada umbral se dispara una unica vez por certificacion: ver
 * la restriccion de unicidad {@code (certificacion_id, tipo_alerta)} en
 * {@code Alerta}.
 */
public enum TipoAlerta {
    DIAS_90(90),
    DIAS_30(30),
    DIAS_7(7);

    private final int dias;

    TipoAlerta(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }
}
