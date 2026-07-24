package com.piedpiper.carbonhub.reconocimiento.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoCertificacionRequestDTO {

    @JsonProperty("usuario_id")
    private UUID usuarioId;

    @JsonProperty("evento_generado")
    private String eventoGenerado;

    @JsonProperty("fecha_evento")
    private Instant fechaEvento;
}
