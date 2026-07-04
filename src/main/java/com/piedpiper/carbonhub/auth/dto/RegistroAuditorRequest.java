package com.piedpiper.carbonhub.auth.dto;

import java.time.LocalDate;

public record RegistroAuditorRequest(
        String idToken,
        String nombreCompleto,
        String numeroCertificacion,
        String entidadCertificadora,
        LocalDate fechaVigenciaCert,
        Integer aniosExperiencia,
        boolean aceptaTerminos) {
}
