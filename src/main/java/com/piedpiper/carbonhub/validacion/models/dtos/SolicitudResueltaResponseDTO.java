package com.piedpiper.carbonhub.validacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResueltaResponseDTO {

    private UUID id;
    private String estado;
    private String estadoAuditor;
    private Instant fechaResolucion;
    private String motivoRechazo;
}
