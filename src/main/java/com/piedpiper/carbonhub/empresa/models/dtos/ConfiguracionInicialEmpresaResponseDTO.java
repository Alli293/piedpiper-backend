package com.piedpiper.carbonhub.empresa.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionInicialEmpresaResponseDTO {

    private UUID empresaId;
    private String nombreEmpresa;
    private String slug;
    private boolean documentosPendientes;
}
