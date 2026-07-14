package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.MetodoTransporte;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;
import com.piedpiper.carbonhub.emision.models.enums.UnidadPeso;

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
@DiscriminatorValue("ENVIO")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionEnvio extends Emision {

    @Column(name = "weight_value", precision = 12, scale = 3)
    private BigDecimal weightValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", length = 10)
    private UnidadPeso weightUnit;

    @Column(name = "distance_value", precision = 12, scale = 3)
    private BigDecimal distanceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private UnidadDistancia distanceUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_method", length = 10)
    private MetodoTransporte transportMethod;

    @Override
    public CategoriaEmision getCategoria() {
        return CategoriaEmision.ENVIO;
    }
}
