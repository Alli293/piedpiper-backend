package com.piedpiper.carbonhub.emision.models.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UnidadElectricidad {

    @JsonProperty("kwh") KWH,
    @JsonProperty("mwh") MWH
}
