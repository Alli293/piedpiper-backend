package com.piedpiper.carbonhub.notification.service;

public interface EmailInvitacionService {

    void enviarCorreoInvitacion(String email, String nombreEmpresa, String token);
}
