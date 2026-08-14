package com.piedpiper.carbonhub.ecoruta.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registros_ponderacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPonderacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "itinerario_id", nullable = false)
    private UUID itinerarioId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "puntuacion_total", nullable = false, precision = 5, scale = 2)
    private BigDecimal puntuacionTotal;

    @Column(name = "componente_certificaciones", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal componenteCertificaciones = BigDecimal.ZERO;

    @Column(name = "componente_ima", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal componenteIma = BigDecimal.ZERO;

    @Column(name = "componente_benchmark", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal componenteBenchmark = BigDecimal.ZERO;

    @Column(name = "cantidad_certificaciones", nullable = false)
    @Builder.Default
    private Integer cantidadCertificaciones = 0;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
