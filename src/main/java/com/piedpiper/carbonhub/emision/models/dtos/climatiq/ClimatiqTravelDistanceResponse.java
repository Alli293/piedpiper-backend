package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClimatiqTravelDistanceResponse(
        BigDecimal co2e,
        @JsonProperty("co2e_unit") String co2eUnit,
        @JsonProperty("distance_km") BigDecimal distanceKm) {
}
