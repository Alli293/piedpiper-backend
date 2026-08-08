package com.piedpiper.carbonhub.auditoria.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAuditoriaResponseDTO {

    private UUID id;
    private String nombreArchivo;
    private long tamanioBytes;
}
