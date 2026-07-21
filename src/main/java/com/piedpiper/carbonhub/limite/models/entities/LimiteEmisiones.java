package com.piedpiper.carbonhub.limite.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "limites_emisiones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_limites_emisiones_empresa_anio",
                columnNames = {"empresa_id", "anio"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimiteEmisiones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "limite_mt", nullable = false, precision = 16, scale = 4)
    private BigDecimal limiteMt;

    @Column(length = 500)
    private String justificacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public LimiteEmisiones(UUID empresaId, Integer anio, BigDecimal limiteMt) {
        this(empresaId, anio, limiteMt, null);
    }

    public LimiteEmisiones(UUID empresaId, Integer anio, BigDecimal limiteMt, String justificacion) {
        this.empresaId = empresaId;
        this.anio = anio;
        this.limiteMt = limiteMt;
        this.justificacion = justificacion;
    }

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
