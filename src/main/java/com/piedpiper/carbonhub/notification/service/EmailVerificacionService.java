package com.piedpiper.carbonhub.notification.service;

public interface EmailVerificacionService {

    void enviarCorreoVerificacion(String nombreDestinatario, String email, String token);
}
