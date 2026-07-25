package com.piedpiper.carbonhub.certificacion.models.dtos;

import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando de emision de una certificacion. Lo construira el dominio de
 * auditorias cuando una auditoria sea aprobada; hoy tambien lo envia el
 * administrador de plataforma al reintentar manualmente una emision fallida.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmitirCertificacionRequestDTO {

    @NotNull(message = "Indique la auditoria de origen.")
    private UUID idAuditoria;

    @NotNull(message = "Indique la empresa auditada.")
    private UUID idEmpresa;

    @NotNull(message = "Indique el auditor que aprobo la auditoria.")
    private UUID idAuditor;

    @NotBlank(message = "Indique el resultado de la auditoria.")
    private String resultadoAuditoria;

    @NotNull(message = "Indique la fecha en que se realizo la auditoria.")
    private LocalDate fechaAuditoria;

    @NotNull(message = "Indique el tipo de certificacion a emitir.")
    private TipoCertificacion tipo;

    /**
     * Opcional: la vigencia la determina el tipo de certificacion, asi que
     * normalmente se deriva. Si el llamante la envia igualmente, se valida que
     * sea posterior a la fecha de la auditoria antes de aceptarla.
     */
    private LocalDate fechaVencimientoCert;
}
