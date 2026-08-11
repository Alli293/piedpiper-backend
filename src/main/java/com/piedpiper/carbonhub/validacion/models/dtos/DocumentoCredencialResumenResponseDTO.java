package com.piedpiper.carbonhub.validacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoCredencialResumenResponseDTO {

    private UUID id;
    private String nombreArchivo;
    private long tamanioBytes;
}
