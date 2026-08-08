package com.piedpiper.carbonhub.auditoria.models.dtos;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila de un listado de solicitudes.
 *
 * <p>Deja fuera los documentos y el historial a proposito: un listado con veinte solicitudes
 * traeria el contenido binario de todos sus adjuntos. Para eso esta el detalle.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAuditoriaResumenResponseDTO {

    private UUID id;
    private TipoCertificacionSolicitud tipoCertificacion;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private EstadoSolicitudAuditoria estado;
    private String estadoDescripcion;
    private Instant fechaCreacion;
    private String nombreEmpresa;
    private UUID idAuditor;
    private String nombreAuditor;
    private Instant fechaAsignacion;
    private Instant fechaAceptacion;
    private int cantidadDocumentos;
}
