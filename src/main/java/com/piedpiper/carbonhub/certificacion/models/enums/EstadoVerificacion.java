package com.piedpiper.carbonhub.certificacion.models.enums;

/**
 * Resultado de una verificacion publica por codigo (PP-68). No incluye
 * {@code no_encontrada}: ese caso no produce un cuerpo -- se responde 404,
 * igual que un codigo mal formado, para no revelar si el codigo existio
 * (ver {@code ConsultaCertificacionService#verificarPorCodigo}).
 */
public enum EstadoVerificacion {
    VALIDA_VIGENTE("valida_vigente"),
    VALIDA_VENCIDA("valida_vencida"),
    REVOCADA("revocada");

    private final String codigo;

    EstadoVerificacion(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
