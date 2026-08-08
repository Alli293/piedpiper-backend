package com.piedpiper.carbonhub.perfilpublico.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnlacePerfilDTO {

    private String urlCanonica;
    private String codigoIncrustar;
    private String qrBase64;
    private String ogTitulo;
    private String ogDescripcion;
    private String ogImagen;
    private String ogUrl;
}
