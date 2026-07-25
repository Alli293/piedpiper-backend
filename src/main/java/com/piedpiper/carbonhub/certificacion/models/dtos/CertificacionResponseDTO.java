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
public class CertificacionResponseDTO {

    private UUID id;
    private UUID idAuditoria;
    private UUID idEmpresa;
    private UUID idAuditor;
    private String tipo;
    private String nombreCertificacion;
    private Instant fechaEmision;
    private LocalDate fechaVencimiento;
    private String estado;

    /** Credencial OpenBadges 3.0 firmada (VC-JWT compacto). */
    private String credencialJwt;

    /** Falso cuando la certificacion ya existia y la emision se omitio. */
    private boolean recienEmitida;
}
