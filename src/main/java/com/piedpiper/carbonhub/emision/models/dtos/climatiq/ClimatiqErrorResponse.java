package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClimatiqErrorResponse(
        String error,
        @JsonProperty("error_code") String errorCode,
        String message) {
}
