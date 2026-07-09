package com.piedpiper.carbonhub.emision.models.entities;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "emisiones")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "categoria", discriminatorType = DiscriminatorType.STRING, length = 40)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class Emision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "fecha_actividad", nullable = false)
    private LocalDate fechaActividad;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "carbon_kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal carbonKg;

    @Column(name = "carbon_mt", nullable = false, precision = 14, scale = 3)
    private BigDecimal carbonMt;

    @Column(name = "factor_emision_id", nullable = false, length = 100)
    private String factorEmisionId;

    @Column(name = "estimated_at", nullable = false)
    private Instant estimatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    public abstract CategoriaEmision getCategoria();
}
