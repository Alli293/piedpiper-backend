package com.piedpiper.carbonhub.certificacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Version recortada de {@link CertificacionResponseDTO} para el listado
 * privado: sin {@code credencialJwt}, que ahi es payload de mas y solo hace
 * falta en el detalle de una certificacion puntual.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificacionResumenResponseDTO {

    private UUID id;
    private UUID idAuditoria;
    private UUID idEmpresa;
    private UUID idAuditor;
    private String tipo;
    private String nombreCertificacion;
    private Instant fechaEmision;
    private LocalDate fechaVencimiento;
    private String estado;
}
