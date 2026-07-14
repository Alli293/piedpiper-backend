package com.piedpiper.carbonhub.emision.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmisionEnvioResponseDTO {

    private UUID id;
    private CategoriaEmision categoria;
    private String titulo;
    private LocalDate fechaActividad;
    private BigDecimal weightValue;
    private UnidadPeso weightUnit;
    private BigDecimal distanceValue;
    private UnidadDistancia distanceUnit;
    private MetodoTransporte transportMethod;
    private BigDecimal carbonKg;
    private BigDecimal carbonMt;
    private String factorEmisionId;
    private Instant estimatedAt;
    private Instant createdAt;
}
