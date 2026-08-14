package com.piedpiper.carbonhub.certificacion.models.entities;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Notificacion visible en el panel del administrador de empresa. El paquete
 * {@code notification} solo cubre correo saliente, asi que esta es la unica
 * notificacion en aplicacion del sistema.
 */
@Entity
@Table(name = "notificaciones_panel")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificacion_id", nullable = false)
    private Certificacion certificacion;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
}
