package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionHuellaDTO {

    private String rangoPeriodo;
    private String tendencia;
    private List<PuntoHuellaDTO> serie;
}
