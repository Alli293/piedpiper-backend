package com.piedpiper.carbonhub.auditoria.models.events;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaFinalizadaEvent(
        UUID auditoriaId,
        UUID auditorId,
        Instant fechaAsignacion,
        Instant fechaPrimeraRespuesta,
        String sectorEmpresa) {
}
