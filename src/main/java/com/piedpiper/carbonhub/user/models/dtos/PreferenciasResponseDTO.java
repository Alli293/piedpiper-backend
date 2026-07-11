package com.piedpiper.carbonhub.user.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasResponseDTO {

    private String idioma;
    private String moneda;
    private String unidades;
}
