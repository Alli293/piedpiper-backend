package com.piedpiper.carbonhub.notification.service;

/**
 * Envio del correo de verificacion de cuenta (PP-33).
 * La implementacion real debe reintentar hasta 3 veces con intervalos de 5 minutos
 * cuando el envio falle; el stub actual no implementa reintentos.
 */
public interface EmailVerificacionService {

    void enviarCorreoVerificacion(String nombreDestinatario, String email, String token);
}
