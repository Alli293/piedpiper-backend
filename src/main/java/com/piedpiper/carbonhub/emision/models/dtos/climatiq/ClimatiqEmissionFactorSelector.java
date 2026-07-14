package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClimatiqEmissionFactorSelector(
        @JsonProperty("activity_id") String activityId,
        @JsonProperty("data_version") String dataVersion,
        String region) {
}
