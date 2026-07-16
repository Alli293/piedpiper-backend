package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionFlotaResponseDTO extends EmisionResponseDTO {

    private TipoVehiculo tipoVehiculo;
    private Combustible combustible;
    private BigDecimal distanceValue;
    private UnidadDistancia distanceUnit;
}
