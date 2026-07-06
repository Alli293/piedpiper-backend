package com.piedpiper.carbonhub.auditor.models.entities;

import com.piedpiper.carbonhub.auditor.models.enums.EstadoSolicitud;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "solicitudes_validacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudValidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditor_id", nullable = false, unique = true)
    private Usuario auditor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud;
}
