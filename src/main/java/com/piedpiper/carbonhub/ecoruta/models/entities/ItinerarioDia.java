package com.piedpiper.carbonhub.ecoruta.models.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itinerarios_dias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioDia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerario_id", nullable = false)
    private Itinerario itinerario;

    @Column(name = "numero_dia", nullable = false)
    private Integer numeroDia;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer orden;

    @OneToMany(mappedBy = "itinerarioDia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ItinerarioActividad> actividades = new ArrayList<>();
}
