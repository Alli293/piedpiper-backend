package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkDTO {

    private UUID empresaId;
    private PosicionBenchmark posicion;
    private BigDecimal valorEmpresa;
    private BigDecimal promedioSector;
    private Instant version;
}
