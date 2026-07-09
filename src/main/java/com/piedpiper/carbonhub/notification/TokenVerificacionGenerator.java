package com.piedpiper.carbonhub.notification;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public final class TokenVerificacionGenerator {

    private static final int LONGITUD_BYTES = 32;
    private static final long HORAS_EXPIRACION = 24;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenVerificacionGenerator() {
    }

    public static String generar() {
        byte[] bytes = new byte[LONGITUD_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static Instant calcularExpiracion() {
        return Instant.now().plus(HORAS_EXPIRACION, ChronoUnit.HOURS);
    }
}
