package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fila liviana para el listado de "Mis itinerarios" (PP-89) — a diferencia de
 * {@link ItinerarioResponseDTO}, nunca trae {@code dias} completo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioResumenResponseDTO {

    private UUID id;
    private Integer cantidadDias;
    private LocalDate fechaInicio;
    private String tipoViaje;
    private BigDecimal ecoScore;
    private String clasificacionAmbiental;
    private boolean ecoScoreParcial;

    /** Distinct de las provincias de todas las actividades — el frontend arma el título de la tarjeta con esto. */
    private List<String> provinciasVisitadas;

    private Instant fechaGeneracion;
    private Instant actualizadoEn;
}
