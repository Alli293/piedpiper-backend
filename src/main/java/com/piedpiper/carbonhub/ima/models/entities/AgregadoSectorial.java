package com.piedpiper.carbonhub.ima.models.entities;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "agregados_sectoriales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sector", "anio", "mes"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgregadoSectorial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SectorIndustrial sector;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "cantidad_empresas", nullable = false)
    private Integer cantidadEmpresas;

    @Column(name = "intensidad_promedio", precision = 14, scale = 6)
    private BigDecimal intensidadPromedio;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
