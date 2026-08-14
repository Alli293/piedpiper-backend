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

    /**
     * Cuantos caben por pagina. Viaja en la respuesta y no queda como constante del cliente porque
     * la pantalla lo necesita para calcular el rango que muestra ("26-32 de 60"): si el servidor
     * cambiara el tamaño y el cliente conservara el suyo, el rango quedaria mal en silencio.
     */
    private int tamanioPagina;
}
