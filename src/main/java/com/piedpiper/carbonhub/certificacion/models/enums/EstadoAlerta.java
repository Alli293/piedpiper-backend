package com.piedpiper.carbonhub.certificacion.models.enums;

/**
 * Estado de una alerta de vencimiento. PP-70 solo genera alertas en estado
 * {@code PENDIENTE}; {@code ENVIADA} y {@code FALLIDA} quedan modeladas para
 * cuando exista un canal de envio real (correo/push) sobre estas alertas.
 */
public enum EstadoAlerta {
    PENDIENTE,
    ENVIADA,
    FALLIDA
}
