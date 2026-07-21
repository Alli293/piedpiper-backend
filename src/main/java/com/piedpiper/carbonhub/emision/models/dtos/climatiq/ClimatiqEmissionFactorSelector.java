package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClimatiqEmissionFactorSelector(
        @JsonProperty("activity_id") String activityId,
        @JsonProperty("data_version") String dataVersion,
        String region) {
}
