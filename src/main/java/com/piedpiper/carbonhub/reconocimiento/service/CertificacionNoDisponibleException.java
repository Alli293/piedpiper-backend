package com.piedpiper.carbonhub.reconocimiento.service;

public class CertificacionNoDisponibleException extends RuntimeException {

    public CertificacionNoDisponibleException(String message) {
        super(message);
    }

    public CertificacionNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
