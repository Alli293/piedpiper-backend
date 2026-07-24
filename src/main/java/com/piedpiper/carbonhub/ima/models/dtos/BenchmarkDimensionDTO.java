package com.piedpiper.carbonhub.ima.models.dtos;

import com.piedpiper.carbonhub.ima.models.enums.PosicionBenchmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkDimensionDTO {

    private BigDecimal valorEmpresa;
    private BigDecimal promedioSector;
    private PosicionBenchmark posicion;
}
