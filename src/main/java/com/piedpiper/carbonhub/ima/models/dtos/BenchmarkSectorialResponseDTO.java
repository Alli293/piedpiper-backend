package com.piedpiper.carbonhub.ima.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkSectorialResponseDTO {

    private boolean benchmarkDisponible;
    private Integer cantidadEmpresas;
    private boolean imaParcial;
    private BenchmarkDimensionDTO ima;
    private BenchmarkDimensionDTO cobertura;
    private BenchmarkDimensionDTO puntajeIntensidadSectorial;
    private BenchmarkDimensionDTO consistencia;
}
