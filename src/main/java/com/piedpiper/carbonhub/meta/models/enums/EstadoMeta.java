package com.piedpiper.carbonhub.meta.models.enums;

/**
 * Estado persistido de una meta. Solo existe {@code ACTIVA} hoy —
 * "vencida" NO es un valor de este enum: es una condición visual derivada
 * en cada consulta comparando {@code fechaLimite} contra la fecha actual
 * (ver {@code MetaService}). Una meta vencida sigue {@code ACTIVA} en base
 * de datos y sigue visible en el listado, tal como pide el criterio de
 * aceptación de PP-78 ("se marca visualmente como 'vencida' ... pero no se
 * elimina del listado"). Mismo patrón que {@code EstadoCertificacion}
 * (activa = no revocada; vigente/vencida = no vencida, son conceptos
 * distintos).
 */
public enum EstadoMeta {
    ACTIVA
}
