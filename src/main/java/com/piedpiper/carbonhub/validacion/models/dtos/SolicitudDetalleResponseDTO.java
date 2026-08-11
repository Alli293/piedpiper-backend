package com.piedpiper.carbonhub.validacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDetalleResponseDTO {

    private UUID id;
    private String nombreAuditor;
    private String email;
    private String estado;
    private Instant fechaSolicitud;
    private Integer aniosExperiencia;
    private List<String> especialidades = List.of();
    private String descripcionProfesional;
    private String sitioWeb;
    private List<DocumentoCredencialResumenResponseDTO> documentos;
}
