package com.piedpiper.carbonhub.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException tokenInvalido() {
        return new ApiException(HttpStatus.UNAUTHORIZED,
                "No fue posible verificar tu cuenta de Google. Por favor, intenta nuevamente.");
    }

    public static ApiException correoNoVerificado() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Tu correo de Google no está verificado. Verifica tu cuenta de Google antes de continuar.");
    }

    public static ApiException cuentaDuplicada(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    public static ApiException googleTimeout() {
        return new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                "El servicio de autenticación no está disponible en este momento. Intenta más tarde.");
    }

    public static ApiException errorInterno(String mensaje) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, mensaje);
    }
}
