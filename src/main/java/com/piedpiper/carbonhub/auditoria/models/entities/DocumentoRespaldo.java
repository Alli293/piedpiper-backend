package com.piedpiper.carbonhub.auditoria.models.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "documentos_respaldo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoRespaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudAuditoria solicitud;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false)
    private String tipoContenido;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    // Sin @Lob a propósito: sobre Postgres, @Lob en un byte[] lo trata como Large Object y guarda
    // un OID en vez de los bytes, lo que rompe el insert contra una columna bytea.
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(name = "fecha_carga", nullable = false)
    private Instant fechaCarga;
}
