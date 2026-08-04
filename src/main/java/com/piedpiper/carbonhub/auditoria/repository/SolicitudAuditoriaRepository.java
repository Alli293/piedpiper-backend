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

    @Query("""
            select s.id from SolicitudAuditoria s
            where s.auditor is not null
              and s.estado = :estadoSinRespuesta
              and s.fechaAsignacion < :limite
            """)
    List<UUID> idsConAsignacionVencida(@Param("estadoSinRespuesta") EstadoSolicitudAuditoria estadoSinRespuesta,
                                       @Param("limite") Instant limite);
}
