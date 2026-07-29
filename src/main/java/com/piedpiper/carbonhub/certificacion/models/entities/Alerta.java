package com.piedpiper.carbonhub.certificacion.models.entities;

import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Alerta de vencimiento de una certificacion, generada por el proceso
 * nocturno (PP-70) al cruzar los umbrales de 90, 30 o 7 dias. Cada umbral
 * se dispara una unica vez por certificacion (ver la restriccion de
 * unicidad de abajo); el panel del dashboard (PP-76) lee estos registros.
 */
@Entity
@Table(
        name = "alertas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alerta_certificacion_tipo",
                columnNames = {"certificacion_id", "tipo_alerta"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alerta", nullable = false, length = 10)
    private TipoAlerta tipoAlerta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoAlerta estado = EstadoAlerta.PENDIENTE;

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;
}
