package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaUsuarioResponseDTO {

    private Long idInsignia;
    private String nombre;
    private String descripcion;
    private String eventoDesbloqueo;
    private Instant fechaObtencion;
}
