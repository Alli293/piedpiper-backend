package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaAuditoresResponseDTO {

    private List<AuditorResumenResponseDTO> contenido;
    private long totalResultados;
    private int paginaActual;
    private int totalPaginas;
}
