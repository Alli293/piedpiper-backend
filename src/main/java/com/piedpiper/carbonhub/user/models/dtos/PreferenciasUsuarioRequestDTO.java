package com.piedpiper.carbonhub.user.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Los campos se reciben como String y se validan contra el catálogo de enums
 * en el servicio, de forma que un valor fuera de catálogo responda 422
 * (con Jackson deserializando enums directamente respondería 400).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasUsuarioRequestDTO {

    @NotBlank(message = "Selecciona un idioma válido.")
    private String idioma;

    @NotBlank(message = "Selecciona una moneda válida.")
    private String moneda;

    @NotBlank(message = "Selecciona un sistema de unidades válido.")
    private String unidades;
}
