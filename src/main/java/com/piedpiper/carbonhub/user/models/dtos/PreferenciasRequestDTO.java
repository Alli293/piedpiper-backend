package com.piedpiper.carbonhub.user.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasRequestDTO {

    // Se reciben como texto y se validan contra el catálogo en el servicio,
    // para poder responder 422 (y no 400 de deserialización) ante valores
    // fuera de catálogo, como exige PP-31.
    @NotBlank(message = "El idioma es obligatorio.")
    private String idioma;

    @NotBlank(message = "La moneda es obligatoria.")
    private String moneda;

    @NotBlank(message = "El sistema de unidades es obligatorio.")
    private String unidades;
}
