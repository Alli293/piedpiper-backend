package com.piedpiper.carbonhub.auditor.models.entities;

import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "perfiles_auditor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilAuditor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_id", nullable = false, unique = true)
    private Usuario auditor;

    @Column(name = "especialidades", nullable = false, length = 500)
    private String especialidades;

    @Column(name = "zonas_cobertura", nullable = false, length = 500)
    private String zonasCobertura;

    @Column(nullable = false)
    private boolean disponible;

    @Column(name = "descripcion_profesional", length = 500)
    private String descripcionProfesional;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
