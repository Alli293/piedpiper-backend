package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Cuerpo genérico para POST /data/v1/estimate. {@code parameters} varía según la categoría
 * (energy/energy_unit para electricidad; weight/distance/... para envíos; etc.), por eso es un mapa
 * en lugar de un tipo fuerte por categoría.
 */
public record ClimatiqEstimateRequest(
        @JsonProperty("emission_factor") ClimatiqEmissionFactorSelector emissionFactor,
        Map<String, Object> parameters) {
}
