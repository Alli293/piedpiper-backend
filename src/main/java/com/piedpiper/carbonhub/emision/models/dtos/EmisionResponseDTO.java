package com.piedpiper.carbonhub.emision.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.DistanceUnit;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmisionResponseDTO {

    private UUID id;
    private CategoriaEmision categoria;
    private String titulo;
    private LocalDate fechaActividad;
    private BigDecimal electricityValue;
    private UnidadElectricidad electricityUnit;
    private Integer passengers;
    private List<VueloLegResponseDTO> legs;
    private DistanceUnit distanceUnit;
    private BigDecimal distanceValue;
    private BigDecimal carbonKg;
    private BigDecimal carbonMt;
    private String factorEmisionId;
    private Instant estimatedAt;
    private Instant createdAt;

    public EmisionResponseDTO(UUID id,
                              CategoriaEmision categoria,
                              String titulo,
                              LocalDate fechaActividad,
                              BigDecimal electricityValue,
                              UnidadElectricidad electricityUnit,
                              BigDecimal carbonKg,
                              BigDecimal carbonMt,
                              String factorEmisionId,
                              Instant estimatedAt,
                              Instant createdAt) {
        this.id = id;
        this.categoria = categoria;
        this.titulo = titulo;
        this.fechaActividad = fechaActividad;
        this.electricityValue = electricityValue;
        this.electricityUnit = electricityUnit;
        this.carbonKg = carbonKg;
        this.carbonMt = carbonMt;
        this.factorEmisionId = factorEmisionId;
        this.estimatedAt = estimatedAt;
        this.createdAt = createdAt;
    }
}
