package com.piedpiper.carbonhub.establecimiento.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de {@code GET /api/establecimientos/{id}/banner} (PP-95). {@code banner} es
 * {@code null} cuando el establecimiento no tiene país registrado, el código no es válido, o
 * countries.dev no está disponible — en los tres casos la respuesta sigue siendo {@code 200},
 * nunca un error (Req PP-95).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstablecimientoBannerResponseDTO {

    private BannerOrigenDTO banner;
}
