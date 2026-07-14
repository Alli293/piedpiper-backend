package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmisionResponseDTO {

    private UUID id;
    private CategoriaEmision categoria;
    private String titulo;
    private LocalDate fechaActividad;
    private BigDecimal electricityValue;
    private UnidadElectricidad electricityUnit;
    private BigDecimal carbonKg;
    private BigDecimal carbonMt;
    private String factorEmisionId;
    private Instant estimatedAt;
    private Instant createdAt;
}
