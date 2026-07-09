package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("ELECTRICIDAD")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionElectricidad extends Emision {

    @Column(name = "electricity_value", precision = 12, scale = 3)
    private BigDecimal electricityValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "electricity_unit", length = 10)
    private UnidadElectricidad electricityUnit;

    @Override
    public CategoriaEmision getCategoria() {
        return CategoriaEmision.ELECTRICIDAD;
    }
}
