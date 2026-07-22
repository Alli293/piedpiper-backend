package com.piedpiper.carbonhub.notification.service;

public interface EmailValidacionAuditorService {

    void enviarResultadoValidacion(String nombre, String email, boolean aprobado, String motivoRechazo);
}
