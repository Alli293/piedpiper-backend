package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioActividadResponseDTO {

    /** Identificador público de la actividad. Expuesto intencionalmente para que el
     *  cliente pueda referenciarlo en PUT /itinerarios/{id}/actividades/{actividadId}/sustituir (PP-92). */
    private UUID id;
    private String nombre;
    private String descripcion;
    private LocalTime horario;
    private Integer duracionMinutos;
    private BigDecimal costoAproximado;
    private String moneda;
    private String establecimientoRecomendado;
    private String provincia;
    private PuntuacionAmbientalResponseDTO puntuacionAmbiental;
    private Integer puntuacionAmbientalEstimada;
}
