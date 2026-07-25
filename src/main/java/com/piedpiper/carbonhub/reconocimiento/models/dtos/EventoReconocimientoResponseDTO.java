package com.piedpiper.carbonhub.reconocimiento.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoReconocimientoResponseDTO {

    private UUID id;

    @JsonProperty("usuario_id")
    private UUID usuarioId;

    @JsonProperty("evento_generado")
    private String eventoGenerado;

    @JsonProperty("fecha_evento")
    private Instant fechaEvento;

    @JsonProperty("estado_envio")
    private EstadoEnvioCertificacion estadoEnvio;
}
