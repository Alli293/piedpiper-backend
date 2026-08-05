package com.piedpiper.carbonhub.auditoria.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reportes_auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudAuditoria solicitud;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false)
    private String tipoContenido;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_carga", nullable = false)
    private Instant fechaCarga;
}
