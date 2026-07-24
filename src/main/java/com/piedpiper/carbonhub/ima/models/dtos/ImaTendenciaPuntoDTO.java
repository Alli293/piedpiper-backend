package com.piedpiper.carbonhub.ima.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImaTendenciaPuntoDTO {

    /** Período del punto en formato ISO {@code YYYY-MM}. */
    private String mes;

    /** IMA de la empresa en ese mes; null cuando no existe snapshot. */
    private BigDecimal imaEmpresa;

    /** Promedio sectorial en ese mes; null cuando el sector no alcanzó 5 empresas. */
    private BigDecimal imaPromedioSector;
}
