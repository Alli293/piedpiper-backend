package com.piedpiper.carbonhub.ecoruta.models.entities;

import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;
import com.piedpiper.carbonhub.ecoruta.models.enums.EstadoItinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "itinerarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Itinerario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "cantidad_dias", nullable = false)
    private Integer cantidadDias;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_viaje", nullable = false, length = 20)
    private TipoViaje tipoViaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoItinerario estado;

    /** Se incrementa en cada regeneración/ajuste (PP-88 la usa para sustituir la versión anterior). */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "puntuacion_ambiental_preliminar", precision = 5, scale = 2)
    private BigDecimal puntuacionAmbientalPreliminar;

    /** EcoScore del itinerario (PP-91): promedio ponderado IMA/indicadores/factor_actividad. */
    @Column(name = "eco_score", precision = 5, scale = 1)
    private BigDecimal ecoScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_ambiental", length = 20)
    private ClasificacionAmbiental clasificacionAmbiental;

    @Column(name = "eco_score_parcial", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean ecoScoreParcial = false;

    @Column(name = "eco_score_calculado_en")
    private Instant ecoScoreCalculadoEn;

    @Column(name = "favorito", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean favorito = false;

    @Column(name = "generado_parcial", nullable = false)
    private boolean generadoParcial;

    @Column(name = "mensaje_parcial", length = 500)
    private String mensajeParcial;

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @OneToMany(mappedBy = "itinerario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ItinerarioDia> dias = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        creadoEn = now;
        actualizadoEn = now;
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = Instant.now();
    }
}
