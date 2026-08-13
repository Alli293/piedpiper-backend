package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionSectorDTO {

    private String sector;
    private BigDecimal porcentaje;
}
