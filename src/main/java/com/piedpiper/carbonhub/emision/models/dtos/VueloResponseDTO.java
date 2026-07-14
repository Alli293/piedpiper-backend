package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.DistanceUnit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VueloResponseDTO extends EmisionResponseDTO {

    private Integer passengers;
    private List<VueloLegResponseDTO> legs;
    private DistanceUnit distanceUnit;
    private BigDecimal distanceValue;
}
