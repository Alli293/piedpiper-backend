package com.piedpiper.carbonhub.establecimiento.service;

import com.piedpiper.carbonhub.establecimiento.models.dtos.BannerOrigenDTO;

import java.util.Optional;

/**
 * Resuelve datos de país (bandera + nombre) para el banner de origen (PP-95). El ticket original
 * especifica REST Countries, pero ese servicio dejó de ser gratis/sin autenticación — ver
 * {@link CountriesDevClientImpl} para el reemplazo elegido y por qué.
 */
public interface CountriesDevClient {

    /**
     * Resuelve el banner de origen para un código ISO 3166-1 alpha-2. Nunca lanza: cualquier
     * falla (código inválido, país no encontrado, timeout, servicio caído) se traduce en
     * {@link Optional#empty()} — ver {@link CountriesDevClientImpl}.
     */
    Optional<BannerOrigenDTO> consultarPais(String codigoIso);
}
