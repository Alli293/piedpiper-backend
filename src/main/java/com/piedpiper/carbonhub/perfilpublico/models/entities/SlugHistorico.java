package com.piedpiper.carbonhub.perfilpublico.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slugs_historicos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlugHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "slug_anterior", nullable = false, unique = true)
    private String slugAnterior;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio;
}
