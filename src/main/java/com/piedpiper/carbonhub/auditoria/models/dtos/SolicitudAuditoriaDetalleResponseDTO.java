package com.piedpiper.carbonhub.auditoria.models.dtos;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
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
    private String nombreEmpresa;
    private List<TransicionEstadoAuditoriaResponseDTO> historial;
}
