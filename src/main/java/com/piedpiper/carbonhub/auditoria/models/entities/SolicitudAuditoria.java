package com.piedpiper.carbonhub.auditoria.models.entities;

import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.ResultadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "solicitudes_auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAuditoria {

    public static final int DESCRIPCION_MAX = 500;
    public static final int MOTIVO_RECHAZO_MIN = 10;
    public static final int MOTIVO_RECHAZO_MAX = 300;
    public static final int OBSERVACIONES_MIN = 20;
    public static final int OBSERVACIONES_MAX = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificacion", nullable = false, length = 20)
    private TipoCertificacionSolicitud tipoCertificacion;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;

    @Column(name = "descripcion_solicitud", length = DESCRIPCION_MAX)
    private String descripcionSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitudAuditoria estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private Usuario auditor;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_asignacion", length = 20)
    private OrigenAsignacion origenAsignacion;

    @Column(name = "fecha_asignacion")
    private Instant fechaAsignacion;

    @Column(name = "fecha_aceptacion")
    private Instant fechaAceptacion;

    /**
     * Motivo y fecha del ultimo rechazo. Sobreviven a la liberacion de la asignacion a proposito:
     * el auditor y su fecha se borran para que la solicitud vuelva a estar disponible, pero la
     * empresa necesita seguir viendo por que le rechazaron la solicitud.
     */
    @Column(name = "motivo_rechazo", length = MOTIVO_RECHAZO_MAX)
    private String motivoRechazo;

    @Column(name = "fecha_rechazo")
    private Instant fechaRechazo;

    @Column(name = "fecha_auditoria_realizada")
    private LocalDate fechaAuditoriaRealizada;

    @Column(name = "fecha_carga_reporte")
    private Instant fechaCargaReporte;

    /**
     * Resultado final que emitio el auditor. Se guarda ademas del estado porque son dos cosas
     * distintas: el estado dice donde quedo la solicitud y sirve para el flujo, mientras que el
     * resultado es la decision del auditor y es lo que la empresa lee. Van juntos por construccion
     * (la transicion los fija en la misma operacion) pero derivar uno del otro obligaria a cada
     * consumidor a conocer la tabla de transiciones.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_auditoria", length = 20)
    private ResultadoAuditoria resultadoAuditoria;

    /** Solo se llena cuando el resultado es con observaciones: es el texto que la empresa corrige. */
    @Column(length = OBSERVACIONES_MAX)
    private String observaciones;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    /**
     * Vigencia que el auditor le da a la certificacion al aprobar. Queda tambien en la solicitud y
     * no solo en la certificacion emitida porque la emision ocurre despues del commit: si esa
     * llamada falla, este es el unico lugar donde el dato sobrevive para el reintento manual.
     */
    @Column(name = "fecha_vencimiento_cert")
    private LocalDate fechaVencimientoCert;

    @OneToOne(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ReporteAuditoria reporteAuditoria;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoRespaldo> documentos = new ArrayList<>();

    // El default es necesario para ddl-auto=update: sin el, Hibernate emite
    // "add column version bigint not null" y Postgres lo rechaza si la tabla ya tiene filas,
    // dejando la columna sin crear y toda consulta a la entidad fallando en tiempo de ejecucion.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    public void agregarDocumento(DocumentoRespaldo documento) {
        documento.setSolicitud(this);
        documentos.add(documento);
    }

    public void reemplazarReporteAuditoria(ReporteAuditoria reporte) {
        if (reporteAuditoria != null) {
            reporteAuditoria.setSolicitud(null);
        }
        reporte.setSolicitud(this);
        reporteAuditoria = reporte;
    }
}
