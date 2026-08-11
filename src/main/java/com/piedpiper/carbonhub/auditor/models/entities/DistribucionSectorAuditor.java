package com.piedpiper.carbonhub.auditor.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionSectorAuditor {

    @Column(name = "sector", nullable = false, length = 50)
    private String sector;

    @Column(name = "cantidad", nullable = false, columnDefinition = "integer default 0")
    private int cantidad;

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 1)
    private BigDecimal porcentaje;
}
