package com.piedpiper.carbonhub.meta.models.entities;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.meta.models.enums.EstadoMeta;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Meta de reducción de huella de carbono de una empresa (PP-78). El
 * progreso y el estado "vencida" NO viven aquí: se calculan en cada
 * consulta a partir de {@code fechaLimite} y de la huella actual del
 * período seleccionado (ver {@code MetaService}), igual que
 * {@code Certificacion} separa "activa" (persistido) de "vigente"
 * (calculado).
 */
@Entity
@Table(name = "metas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "nombre_meta", nullable = false, length = 100)
    private String nombreMeta;

    @Column(name = "valor_objetivo_huella", nullable = false, precision = 14, scale = 4)
    private BigDecimal valorObjetivoHuellaT;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMeta estado = EstadoMeta.ACTIVA;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
}
