package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClimatiqEstimateResponse(
        BigDecimal co2e,
        @JsonProperty("co2e_unit") String co2eUnit,
        @JsonProperty("emission_factor") EmissionFactor emissionFactor) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmissionFactor(
            String id,
            @JsonProperty("activity_id") String activityId,
            String region,
            Integer year) {
    }
}
