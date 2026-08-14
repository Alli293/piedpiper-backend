package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoPriorizacion {

    private List<EstablecimientoRankeado> establecimientosRankeados;
    private int establecimientosEvaluados;
    private int indicadoresNoDisponibles;
}
