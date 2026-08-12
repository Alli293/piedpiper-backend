package com.piedpiper.carbonhub.auditoria.models.events;

import java.util.UUID;

public record AuditoriaFinalizadaEvent(
        UUID auditoriaId,
        UUID auditorId) {
}
