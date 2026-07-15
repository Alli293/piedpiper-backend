package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionEnvioResponseDTO extends EmisionResponseDTO {

    private BigDecimal weightValue;
    private UnidadPeso weightUnit;
    private BigDecimal distanceValue;
    private UnidadDistancia distanceUnit;
    private MetodoTransporte transportMethod;
}
