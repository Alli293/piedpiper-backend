package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Misma forma que {@code PaginaSolicitudesAuditoriaResponseDTO}, para paginar igual en toda la app. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaItinerariosResponseDTO {

    private List<ItinerarioResumenResponseDTO> contenido;
    private long totalResultados;

    /** Base 1, como la ve el usuario. La conversión a la base 0 de Spring Data vive en el servicio. */
    private int paginaActual;

    private int totalPaginas;
    private int tamanioPagina;
}
