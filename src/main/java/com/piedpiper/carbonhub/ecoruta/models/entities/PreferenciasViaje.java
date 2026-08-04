package com.piedpiper.carbonhub.ecoruta.models.entities;

import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoViaje;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "preferencias_viaje",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_preferencias_viaje_usuario",
                columnNames = {"usuario_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasViaje {

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

    @Column(length = 100)
    private String presupuesto;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "preferencias_viaje_intereses",
            joinColumns = @JoinColumn(name = "preferencias_viaje_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "interes", nullable = false, length = 30)
    @Builder.Default
    private List<InteresTuristico> intereses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "provincia_preferida", length = 20)
    private Provincia provinciaPreferida;

    @Column(name = "ubicacion_actual", length = 200)
    private String ubicacionActual;

    @Column(name = "buscar_cerca_de_mi", nullable = false)
    private boolean buscarCercaDeMi;

    @Column(name = "limitaciones_movilidad", length = 500)
    private String limitacionesMovilidad;

    @Column(name = "requiere_hospedaje", nullable = false)
    private boolean requiereHospedaje;

    // Rate limiting de generación de itinerarios vive aquí (no en Usuario.java, como en PP-29)
    // porque es un límite específico del dominio EcoRuta, no una propiedad general de la cuenta.
    /** Contador del rate limit de generación de itinerarios (PP-85). Ventana fija de 1 hora. */
    @Column(name = "itinerario_generacion_contador", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int itinerarioGeneracionContador = 0;

    @Column(name = "itinerario_generacion_ventana_inicio")
    private Instant itinerarioGeneracionVentanaInicio;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

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
