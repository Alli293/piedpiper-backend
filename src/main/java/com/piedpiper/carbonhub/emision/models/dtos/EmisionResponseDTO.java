package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class EmisionResponseDTO {

    private UUID id;
    private CategoriaEmision categoria;
    private String titulo;
    private LocalDate fechaActividad;
    private BigDecimal carbonKg;
    private BigDecimal carbonMt;
    private String factorEmisionId;
    private Instant estimatedAt;
    private Instant createdAt;
}
