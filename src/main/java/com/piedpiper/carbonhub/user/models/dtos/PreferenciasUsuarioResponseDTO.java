package com.piedpiper.carbonhub.user.models.dtos;

import com.piedpiper.carbonhub.user.models.enums.Idioma;
import com.piedpiper.carbonhub.user.models.enums.Moneda;
import com.piedpiper.carbonhub.user.models.enums.UnidadesMedida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasUsuarioResponseDTO {

    private String idioma;
    private String moneda;
    private String unidades;

    public static PreferenciasUsuarioResponseDTO de(Idioma idioma, Moneda moneda,
                                                    UnidadesMedida unidades) {
        return new PreferenciasUsuarioResponseDTO(idioma.name(), moneda.name(), unidades.name());
    }
}
