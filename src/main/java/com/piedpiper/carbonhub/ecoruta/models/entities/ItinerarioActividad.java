package com.piedpiper.carbonhub.ecoruta.models.entities;

import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Moneda;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;

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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "itinerarios_actividades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerario_dia_id", nullable = false)
    private ItinerarioDia itinerarioDia;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalTime horario;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "costo_aproximado", precision = 12, scale = 2)
    private BigDecimal costoAproximado;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Moneda moneda;

    @Column(name = "establecimiento_recomendado", length = 200)
    private String establecimientoRecomendado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provincia provincia;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_turistica", length = 30)
    private InteresTuristico categoriaTuristica;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "puntuacion_ambiental_estimada")
    private Integer puntuacionAmbientalEstimada;
}
