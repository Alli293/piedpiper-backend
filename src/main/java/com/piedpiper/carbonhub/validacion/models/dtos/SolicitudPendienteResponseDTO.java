package com.piedpiper.carbonhub.validacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudPendienteResponseDTO {

    private UUID id;
    private String nombreAuditor;
    private String email;
    private Instant fechaSolicitud;
}
