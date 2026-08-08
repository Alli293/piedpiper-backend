package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SolicitudAuditoriaRepository extends JpaRepository<SolicitudAuditoria, UUID> {

    @Query("""
            select count(s) > 0 from SolicitudAuditoria s
            where s.empresa.id = :empresaId
              and s.estado not in :estadosCerrados
              and s.periodoInicio <= :periodoFin
              and :periodoInicio <= s.periodoFin
            """)
    boolean existeSolicitudEnCursoTraslapada(@Param("empresaId") UUID empresaId,
                                             @Param("estadosCerrados") Collection<EstadoSolicitudAuditoria> estadosCerrados,
                                             @Param("periodoInicio") LocalDate periodoInicio,
                                             @Param("periodoFin") LocalDate periodoFin);

    /**
     * Trae empresa y auditor de una vez: el listado los muestra en cada fila y sin el fetch join
     * cada solicitud dispararia dos consultas mas.
     */
    @Query("""
            select distinct s from SolicitudAuditoria s
            left join fetch s.empresa
            left join fetch s.auditor
            where s.empresa.id = :empresaId
            order by s.fechaCreacion desc
            """)
    List<SolicitudAuditoria> listarPorEmpresa(@Param("empresaId") UUID empresaId);

    @Query("""
            select distinct s from SolicitudAuditoria s
            left join fetch s.empresa
            left join fetch s.auditor
            where s.auditor.id = :auditorId
            order by s.fechaAsignacion desc
            """)
    List<SolicitudAuditoria> listarAsignadasA(@Param("auditorId") UUID auditorId);

    @Query("""
            select s.id from SolicitudAuditoria s
            where s.auditor is not null
              and s.estado = :estadoSinRespuesta
              and s.fechaAsignacion < :limite
            """)
    List<UUID> idsConAsignacionVencida(@Param("estadoSinRespuesta") EstadoSolicitudAuditoria estadoSinRespuesta,
                                       @Param("limite") Instant limite);
}
