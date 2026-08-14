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
import jakarta.persistence.Version;
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

    /**
     * {@code @Version} real de JPA (antes un {@code @Column} plano incrementado a mano en cada
     * regeneración/ajuste): Hibernate la incrementa solo y la valida en cada
     * {@code UPDATE ... WHERE id = ? AND version = ?} — si otra sesión ya guardó una versión más
     * nueva entre el load y el save de esta, el UPDATE afecta 0 filas y Hibernate lanza
     * {@code ObjectOptimisticLockingFailureException} en vez de pisar el cambio en silencio. Esto
     * cierra la ventana de carrera que la validación manual contra
     * {@code ConversacionContextoDTO.versionItinerario} (chequeada antes de llamar a Gemini, más
     * barata porque falla rápido) no puede cubrir por sí sola: esa validación solo detecta que el
     * *cliente* mandó una versión vieja, no una modificación concurrente que ocurra durante esta
     * misma petición. Señalado en revisión (PR #93) junto con el pedido de separar refinar() en
     * transacciones cortas de snapshot/guardado — esta es la mitigación específica del riesgo de
     * escritura perdida que motivaba ese pedido; la separación transaccional en sí (que apunta al
     * uso de open-in-view, deuda ya documentada en CONVENTIONS.md §12 para todo el codebase) queda
     * fuera de esta ronda a propósito, no en silencio: es un cambio de mayor alcance sobre código
     * ya frágil (ver el bug de orphanRemoval en refinar()) y toca a generar() por el mismo patrón.
     */
    @Version
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
