package com.piedpiper.carbonhub.common;

import java.time.ZoneId;

/**
 * Zonas horarias usadas para calculos de fecha de negocio (no de timestamps
 * tecnicos, que siempre son {@code Instant}). Centralizada aqui porque ya
 * hay mas de un punto del dominio que necesita "el dia de hoy segun Costa
 * Rica" y no el dia segun la zona de la JVM del contenedor.
 */
public final class ZonasHorarias {

    private ZonasHorarias() {
    }

    public static final ZoneId COSTA_RICA = ZoneId.of("America/Costa_Rica");
}
