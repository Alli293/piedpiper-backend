package com.piedpiper.carbonhub.certificacion.models.enums;

/**
 * Umbrales de vencimiento evaluados por el proceso nocturno de alertas
 * (PP-70). Cada umbral se dispara una unica vez por certificacion: ver
 * la restriccion de unicidad {@code (certificacion_id, tipo_alerta)} en
 * {@code Alerta}.
 *
 * <p>{@code codigo} sigue el mismo patron que {@code TipoCertificacion}:
 * el nombre del enum ({@code DIAS_90}) es lo que se persiste en BD
 * ({@code @Enumerated(EnumType.STRING)}), y {@code codigo} es la forma
 * ({@code "90_dias"}) que consumen otros modulos (p. ej. PP-71) en DTOs.</p>
 */
public enum TipoAlerta {
    DIAS_90(90, "90_dias"),
    DIAS_30(30, "30_dias"),
    DIAS_7(7, "7_dias");

    private final int dias;
    private final String codigo;

    TipoAlerta(int dias, String codigo) {
        this.dias = dias;
        this.codigo = codigo;
    }

    public int getDias() {
        return dias;
    }

    public String getCodigo() {
        return codigo;
    }
}
