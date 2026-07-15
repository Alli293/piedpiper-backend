package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("VUELO")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmisionVuelo extends Emision {

    @Column(name = "passengers")
    private Integer passengers;

    @Enumerated(EnumType.STRING)
    @Column(name = "distance_unit", length = 10)
    private UnidadDistancia distanceUnit;

    @Column(name = "distance_value", precision = 14, scale = 3)
    private BigDecimal distanceValue;

    @OneToMany(mappedBy = "emisionVuelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmisionVueloLeg> legs = new ArrayList<>();

    @Override
    public CategoriaEmision getCategoria() {
        return CategoriaEmision.VUELO;
    }

    public void addLeg(EmisionVueloLeg leg) {
        legs.add(leg);
        leg.setEmisionVuelo(this);
    }
}
