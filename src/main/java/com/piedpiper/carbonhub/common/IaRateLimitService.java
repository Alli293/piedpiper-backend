package com.piedpiper.carbonhub.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IaRateLimitService {

    private final ConcurrentHashMap<UUID, Ventana> ventanas = new ConcurrentHashMap<>();
    private final int maxSolicitudes;
    private final Duration duracionVentana;

    public IaRateLimitService(
            @Value("${ia.rate-limit.max-requests:10}") int maxSolicitudes,
            @Value("${ia.rate-limit.window-ms:3600000}") long ventanaMs) {
        this.maxSolicitudes = maxSolicitudes;
        this.duracionVentana = Duration.ofMillis(ventanaMs);
    }

    public boolean reservar(UUID propietarioId) {
        Instant ahora = Instant.now();
        Ventana resultado = ventanas.compute(propietarioId, (id, actual) -> {
            if (actual == null || actual.inicio().plus(duracionVentana).isBefore(ahora)) {
                return new Ventana(ahora, 1, true);
            }
            if (actual.contador() >= maxSolicitudes) {
                return new Ventana(actual.inicio(), actual.contador(), false);
            }
            return new Ventana(actual.inicio(), actual.contador() + 1, true);
        });
        if (ventanas.size() > 10_000) {
            ventanas.entrySet().removeIf(entry ->
                    entry.getValue().inicio().plus(duracionVentana).isBefore(ahora));
        }
        return resultado.permitida();
    }

    private record Ventana(Instant inicio, int contador, boolean permitida) {
    }
}
