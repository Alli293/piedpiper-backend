package com.piedpiper.carbonhub.auditoria.models.entities;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoNotificacionTransicion;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Notificacion pendiente de envio por un cambio de estado de una solicitud de auditoria.
 *
 * <p>La historia pide reintentar hasta tres veces cada cinco minutos si el envio falla, y eso no se
 * puede sostener con un {@code afterCommit}: ese enganche vive en memoria y un reinicio entre el
 * commit y el envio se lleva la notificacion sin dejar rastro. Persistirla es lo que permite que un
 * barrido la recupere despues de un reinicio. Es la desviacion que {@code docs/CONVENTIONS.md} 4.5
 * documenta, con PP-71 como referencia.</p>
 *
 * <p>Los datos del destinatario se guardan desnormalizados porque el envio ocurre despues, fuera de
 * la transaccion que creo la fila: para entonces la solicitud pudo haber cambiado de auditor, y lo
 * que hay que notificar es lo que era cierto cuando ocurrio la transicion.</p>
 */
@Entity
@Table(name = "notificaciones_transicion_auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionTransicionAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudAuditoria solicitud;

    @Column(name = "destinatario_email", nullable = false, length = 254)
    private String destinatarioEmail;

    @Column(name = "destinatario_nombre", length = 150)
    private String destinatarioNombre;

    @Column(name = "nombre_empresa", length = 150)
    private String nombreEmpresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 30)
    private EstadoSolicitudAuditoria estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoSolicitudAuditoria estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacionTransicion estado;

    @Column(name = "intentos_envio", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int intentosEnvio = 0;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;
}
