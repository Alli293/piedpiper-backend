package com.piedpiper.carbonhub.common;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IaRateLimitServiceTest {

    @Test
    void reservaHastaElMaximoYRechazaElExceso() {
        IaRateLimitService service = new IaRateLimitService(2, 60_000);
        UUID propietario = UUID.randomUUID();

        assertThat(service.reservar(propietario)).isTrue();
        assertThat(service.reservar(propietario)).isTrue();
        assertThat(service.reservar(propietario)).isFalse();
    }

    @Test
    void mantieneCuotasIndependientesPorPropietario() {
        IaRateLimitService service = new IaRateLimitService(1, 60_000);

        assertThat(service.reservar(UUID.randomUUID())).isTrue();
        assertThat(service.reservar(UUID.randomUUID())).isTrue();
    }
}
