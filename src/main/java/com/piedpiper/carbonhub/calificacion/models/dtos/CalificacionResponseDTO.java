package com.piedpiper.carbonhub.calificacion.models.dtos;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalificacionResponseDTO {

    private UUID id;
    private UUID auditoriaId;
    private UUID auditorId;
    private UUID empresaId;
    private int calificacion;
    private String comentario;
    private Instant creadoEn;
    private Instant actualizadoEn;
}
