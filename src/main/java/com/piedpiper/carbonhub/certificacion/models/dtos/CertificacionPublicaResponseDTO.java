package com.piedpiper.carbonhub.certificacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificacionPublicaResponseDTO {

    private UUID id;
    private String tipo;
    private String nombreCertificacion;
    private Instant fechaEmision;
    private LocalDate fechaVencimiento;
    private String estado;
    private String codigoVerificacion;
}
