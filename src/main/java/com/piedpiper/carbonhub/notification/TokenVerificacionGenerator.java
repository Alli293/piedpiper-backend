package com.piedpiper.carbonhub.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en este entorno.", e);
        }
    }

    public static Instant calcularExpiracion() {
        return calcularExpiracion(HORAS_EXPIRACION);
    }

    public static Instant calcularExpiracion(long horas) {
        return Instant.now().plus(horas, ChronoUnit.HOURS);
    }
}
