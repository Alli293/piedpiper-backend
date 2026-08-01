package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusquedaPerfilPublicoDTO {

    private String nombreEmpresa;
    private String slug;
    private String sectorIndustrial;
    private String nivelEcologico;
}
