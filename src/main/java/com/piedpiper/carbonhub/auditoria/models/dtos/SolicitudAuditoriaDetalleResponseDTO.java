package com.piedpiper.carbonhub.auditoria.models.dtos;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.ResultadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de una solicitud con su linea de tiempo.
 *
 * <p>Repite los campos de {@link SolicitudAuditoriaResponseDTO} en vez de heredarlos: la convencion
 * reserva la herencia de DTOs para familias con jerarquia real ({@code Emision*ResponseDTO}), y
 * estos dos no lo son, son la misma entidad vista con y sin historial.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAuditoriaDetalleResponseDTO {

    private UUID id;
    private TipoCertificacionSolicitud tipoCertificacion;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String descripcionSolicitud;
    private EstadoSolicitudAuditoria estado;
    private String estadoDescripcion;
    private Instant fechaCreacion;
    private List<DocumentoRespaldoResponseDTO> documentos;
    private UUID idAuditor;
    private AuditorAsignadoResponseDTO auditor;
    private OrigenAsignacion origenAsignacion;
    private Instant fechaAsignacion;
    private Instant fechaAceptacion;

    /**
     * El motivo del ultimo rechazo sigue visible aunque la asignacion ya se haya liberado: es lo
     * que le explica a la empresa por que su solicitud volvio a quedar sin auditor.
     */
    private String motivoRechazo;

    private Instant fechaRechazo;
    private ReporteAuditoriaResponseDTO reporteAuditoria;
    private LocalDate fechaAuditoriaRealizada;
    private Instant fechaCargaReporte;

    /**
     * Resultado final del auditor. Viaja junto al estado y no derivado de el porque la pantalla
     * muestra las dos cosas distintas: en que paso quedo la solicitud y que decidio el auditor.
     */
    private ResultadoAuditoria resultadoAuditoria;

    private String observaciones;
    private Instant fechaResolucion;
    private LocalDate fechaVencimientoCert;
    private String nombreEmpresa;
    private List<TransicionEstadoAuditoriaResponseDTO> historial;
}
