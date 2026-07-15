package com.piedpiper.carbonhub.validacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginaSolicitudesResponseDTO {

    private List<SolicitudPendienteResponseDTO> contenido;
    private int pagina;
    private int totalPaginas;
    private long totalElementos;
}
