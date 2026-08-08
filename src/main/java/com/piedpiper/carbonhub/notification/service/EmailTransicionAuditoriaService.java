package com.piedpiper.carbonhub.notification.service;

public interface EmailTransicionAuditoriaService {

    void enviarCambioEstado(String destinatario,
                            String nombreDestinatario,
                            String nombreEmpresa,
                            String estadoLegible,
                            String urlDetalle);
}
