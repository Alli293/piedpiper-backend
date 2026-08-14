package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilPublicoResponseDTO {

    private String nombreEmpresa;
    private String logoUrl;
    private String sectorIndustrial;
    private String pais;
    private String nivelEcologico;
    private Instant fechaActualizacionNivel;
    private int certificacionesVigentes;
    private int insigniasActivas;
}
