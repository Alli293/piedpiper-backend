package com.piedpiper.carbonhub.notification.service;

import java.time.LocalDate;

public interface EmailAlertaVencimientoService {

    void enviarAlertaVencimiento(String email,
                                 String nombreEmpresa,
                                 String nombreCertificacion,
                                 LocalDate fechaVencimiento,
                                 long diasRestantes,
                                 String urlCertificacion);
}
