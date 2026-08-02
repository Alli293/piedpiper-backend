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

    /**
     * Computado a partir de {@code fechaVencimiento} vs. hoy, no persistido:
     * {@code estado} representa si fue revocada (hoy siempre ACTIVA, no existe
     * revocar todavia), no si vencio. Una certificacion puede seguir ACTIVA y
     * ya no estar vigente.
     */
    private boolean vigente;

    /** URL publica de verificacion, sin autenticacion (ver CertificacionEmisorController). */
    private String urlVerificacion;
}
