package com.piedpiper.carbonhub.auditoria.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "contenidos_reportes_auditoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoReporteAuditoria {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "reporte_auditoria_id", nullable = false)
    private ReporteAuditoria reporteAuditoria;

    // Sin @Lob por el mismo motivo que DocumentoRespaldo: en Postgres debe persistir como bytea.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "contenido", nullable = false, columnDefinition = "bytea")
    private byte[] contenido;
}
