package com.piedpiper.carbonhub.notification.service;

public interface EmailAsignacionAuditorService {

    void enviarAsignacion(String nombreAuditor, String emailAuditor, String nombreEmpresa);

    void enviarExpiracionAsignacion(String correoEmpresa, String nombreEmpresa, String nombreAuditor);
}
