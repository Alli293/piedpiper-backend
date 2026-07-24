package com.piedpiper.carbonhub.ima.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImaTendenciaResponseDTO {

    /** Tamaño de la ventana solicitada, en meses. */
    private Integer mesesAtras;

    /** Serie ordenada del mes más antiguo al más reciente. */
    private List<ImaTendenciaPuntoDTO> serie;

    /** true cuando en toda la ventana el sector nunca alcanzó el mínimo de empresas. */
    private boolean sinDatosSectoriales;

    /** Eventos detectados sobre la serie; vacía si no hay ninguno (no es error). */
    private List<ImaEventoDTO> eventos;
}
