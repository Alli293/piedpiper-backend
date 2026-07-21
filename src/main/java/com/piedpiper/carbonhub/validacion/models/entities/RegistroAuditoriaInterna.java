package com.piedpiper.carbonhub.validacion.models.entities;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registros_auditoria_interna")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditoriaInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo_evento", nullable = false, updatable = false, length = 40)
    private String tipoEvento;

    @Column(name = "solicitud_id", nullable = false, updatable = false)
    private UUID solicitudId;

    @Column(nullable = false, updatable = false, length = 20)
    private String decision;

    @Column(name = "administrador_id", nullable = false, updatable = false)
    private UUID administradorId;

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private Instant fechaEvento;
}
