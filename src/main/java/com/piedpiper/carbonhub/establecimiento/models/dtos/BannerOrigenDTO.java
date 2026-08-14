package com.piedpiper.carbonhub.establecimiento.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de bandera y país de origen para el banner de un establecimiento (PP-95). {@code null}
 * cuando el país no se pudo resolver contra countries.dev — la ausencia de banner nunca es un
 * error, ver {@link com.piedpiper.carbonhub.establecimiento.service.CountriesDevClient}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerOrigenDTO {

    private String nombrePais;
    private String banderaEmoji;
    private String banderaUrlSvg;
    private String codigoIso;
}
