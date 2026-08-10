package com.piedpiper.carbonhub.auditoria.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Misma forma que {@code PaginaAuditoresResponseDTO}, para que el front pagine igual en todas partes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaSolicitudesAuditoriaResponseDTO {

    private List<SolicitudAuditoriaResumenResponseDTO> contenido;
    private long totalResultados;

    /** Base 1, como la ve el usuario. La conversion a la base 0 de Spring Data vive en el servicio. */
    private int paginaActual;

    private int totalPaginas;
}
