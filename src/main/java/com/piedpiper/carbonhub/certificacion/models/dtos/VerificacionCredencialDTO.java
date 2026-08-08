package com.piedpiper.carbonhub.certificacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Resultado de verificar publicamente una credencial por su
 * {@code codigoVerificacion} (PP-68, extendido a insignias). Solo expone
 * datos publicos: del auditor, unicamente su nombre -- no existe todavia en
 * {@code Usuario} un numero de certificacion profesional que tambien exponer.
 *
 * <p>Comun a certificaciones e insignias, distinguidas por {@code categoria}
 * ({@code CERTIFICACION} o {@code INSIGNIA}). {@code auditor} y
 * {@code fechaVencimiento} quedan nulos para una insignia (no tiene auditor
 * ni vence); {@code nivelInsignia} queda nulo para una certificacion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionCredencialDTO {

    private String estado;
    private String categoria;
    private String tipo;
    private String nombreCertificacion;
    private String nivelInsignia;
    private String empresa;
    private String auditor;
    private String entidadCertificadora;
    private Instant fechaEmision;
    private LocalDate fechaVencimiento;

    /** Solo presente cuando {@code estado} es {@code revocada}. */
    private Instant fechaRevocacion;

    private Instant fechaConsulta;
}
