package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionElectricidadResponseDTO extends EmisionResponseDTO {

    private BigDecimal electricityValue;
    private UnidadElectricidad electricityUnit;
}
