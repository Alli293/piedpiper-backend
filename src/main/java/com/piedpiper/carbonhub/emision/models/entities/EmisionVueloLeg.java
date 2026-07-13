package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CabinClass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "emisiones_vuelo_legs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmisionVueloLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emision_id", nullable = false)
    private EmisionVuelo emisionVuelo;

    @Column(name = "departure_airport", nullable = false, length = 3)
    private String departureAirport;

    @Column(name = "destination_airport", nullable = false, length = 3)
    private String destinationAirport;

    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class", nullable = false, length = 20)
    private CabinClass cabinClass;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
