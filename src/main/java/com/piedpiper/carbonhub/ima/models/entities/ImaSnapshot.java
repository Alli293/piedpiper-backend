package com.piedpiper.carbonhub.ima.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ima_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "anio", "mes"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImaSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal cobertura;

    @Column(name = "puntaje_intensidad_sectorial", precision = 5, scale = 1)
    private BigDecimal puntajeIntensidadSectorial;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal consistencia;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal ima;

    @Column(nullable = false)
    private boolean parcial;

    @Column(name = "motivo_parcial")
    private String motivoParcial;

    @Column(precision = 14, scale = 6)
    private BigDecimal intensidad;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "interpretacion_ia", columnDefinition = "TEXT")
    private String interpretacionIa;
}
