package com.piedpiper.carbonhub.notification.service;

public interface EmailResetContrasenaService {

    void enviarResetContrasena(String nombreDestinatario, String email, String token);

    void enviarUsaGoogle(String nombreDestinatario, String email);
}
