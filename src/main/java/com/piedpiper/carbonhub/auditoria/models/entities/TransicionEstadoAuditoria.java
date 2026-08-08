package com.piedpiper.carbonhub.auditoria.models.entities;

import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Registro inmutable de una transicion de estado de una solicitud de auditoria.
 *
 * <p><strong>Desviacion deliberada de la convencion de entidades:</strong> no lleva {@code @Setter}
 * y todas las columnas son {@code updatable = false}. La historia pide que ningun rol pueda
 * modificar ni eliminar estos registros, y una entidad con setters deja esa garantia dependiendo de
 * que nadie los llame. Sin setters, un intento de reescribir el historial no compila.</p>
 *
 * <p>El responsable se guarda desnormalizado ({@code responsableId} y {@code responsableNombre}) en
 * vez de con una relacion a {@code Usuario}: es un registro de auditoria y tiene que seguir siendo
 * legible aunque la cuenta se renombre o se elimine. El nombre es el que tenia la persona en el
 * momento del cambio, que es justamente lo que interesa al leer una linea de tiempo. Para las
 * transiciones del proceso automatico el id queda nulo y el actor es {@code SISTEMA}.</p>
 */
@Entity
@Table(name = "transiciones_estado_auditoria")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransicionEstadoAuditoria {

    public static final int RESPONSABLE_NOMBRE_MAX = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, updatable = false)
    private SolicitudAuditoria solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 30, updatable = false)
    private EstadoSolicitudAuditoria estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30, updatable = false)
    private EstadoSolicitudAuditoria estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private EventoTransicionAuditoria evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ActorTransicionAuditoria actor;

    @Column(name = "responsable_id", updatable = false)
    private UUID responsableId;

    @Column(name = "responsable_nombre", length = RESPONSABLE_NOMBRE_MAX, updatable = false)
    private String responsableNombre;

    @Column(nullable = false, updatable = false)
    private Instant fecha;
}
