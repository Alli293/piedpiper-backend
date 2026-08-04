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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAuditoriaResponseDTO {

    private UUID id;
    private TipoCertificacionSolicitud tipoCertificacion;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String descripcionSolicitud;
    private EstadoSolicitudAuditoria estado;
    private Instant fechaCreacion;
    private List<DocumentoRespaldoResponseDTO> documentos;
    private UUID idAuditor;
    private AuditorAsignadoResponseDTO auditor;
    private OrigenAsignacion origenAsignacion;
    private Instant fechaAsignacion;
}
