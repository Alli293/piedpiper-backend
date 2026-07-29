package com.piedpiper.carbonhub.insignia.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaEmpresaResponseDTO {

    private Long idInsignia;
    private String nivelInsignia;
    private String nombre;
    private String descripcion;
    private Instant fechaObtencion;
}
