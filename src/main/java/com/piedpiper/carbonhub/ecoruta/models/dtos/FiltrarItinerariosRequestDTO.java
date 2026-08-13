package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filtros del listado de "Mis itinerarios" (PP-89). Nombre genérico a propósito: PP-90
 * (favoritos) solo necesita sumarle un campo acá, no renombrar nada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltrarItinerariosRequestDTO {

    private Integer pagina;
}
