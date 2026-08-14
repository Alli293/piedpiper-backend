package com.piedpiper.carbonhub.ecoruta.models.enums;

/**
 * Tipo de recomendación ambiental (PP-93). Hoy solo existe {@code ACTIVIDAD_ALTERNATIVA}; un
 * {@code ESTABLECIMIENTO} (recomendar cambiar de establecimiento sin cambiar la actividad) está
 * mencionado como trabajo futuro pero no implementado — agregarlo acá cuando corresponda evita que
 * productor ({@link com.piedpiper.carbonhub.ecoruta.service.RecomendacionAmbientalService}) y
 * consumidor (frontend) diverjan en el valor del string.
 */
public enum TipoRecomendacion {
    ACTIVIDAD_ALTERNATIVA
}
