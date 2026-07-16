package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;

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
@DiscriminatorValue("FLOTA")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionFlota extends Emision {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", length = 40)
    private TipoVehiculo tipoVehiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "combustible", length = 20)
    private Combustible combustible;

    @Column(name = "distance_value", precision = 12, scale = 3)
    private BigDecimal distanceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private UnidadDistancia distanceUnit;

    @Override
    public CategoriaEmision getCategoria() {
        return CategoriaEmision.FLOTA;
    }
}
