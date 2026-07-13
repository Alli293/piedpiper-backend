package com.piedpiper.carbonhub.emision.models.dtos.climatiq;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClimatiqTravelDistanceRequest(
        @JsonProperty("travel_mode") String travelMode,
        Location origin,
        Location destination,
        Integer year) {

    public static ClimatiqTravelDistanceRequest air(String originIata, String destinationIata, Integer year) {
        return new ClimatiqTravelDistanceRequest(
                "air", new Location(originIata), new Location(destinationIata), year);
    }

    public record Location(String iata) {
    }
}
