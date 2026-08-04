package com.piedpiper.carbonhub.ecoruta.models.dtos;

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
public class ItinerarioResponseDTO {

    private UUID id;
    private Integer cantidadDias;
    private LocalDate fechaInicio;
    private String tipoViaje;
    private String estado;
    private Integer version;
    private BigDecimal puntuacionAmbientalPreliminar;
    private Instant fechaGeneracion;
    private boolean generadoParcial;
    private String mensajeParcial;
    private List<ItinerarioDiaResponseDTO> dias;
}
