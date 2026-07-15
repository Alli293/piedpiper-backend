package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.DistanceUnit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VueloResponseDTO extends EmisionResponseDTO {

    private Integer passengers;
    private List<VueloLegResponseDTO> legs;
    private DistanceUnit distanceUnit;
    private BigDecimal distanceValue;
}
