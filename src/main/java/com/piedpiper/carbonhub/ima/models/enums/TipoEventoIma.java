package com.piedpiper.carbonhub.ima.models.enums;

/** Tipos de evento que se detectan sobre la serie histórica del IMA. */
public enum TipoEventoIma {

    /** El IMA de la empresa cruzó el promedio sectorial, hacia arriba o hacia abajo. */
    CRUCE_SECTOR,

    /** Mes con la mayor variación absoluta del IMA respecto al mes anterior. */
    MAYOR_VARIACION,

    /** Mes sin ningún registro de emisión dentro de la ventana. */
    HUECO_DATOS,

    /** Mes en que aparece por primera vez una categoría de emisión. */
    NUEVA_CATEGORIA
}
