package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            select distinct s from SolicitudAuditoria s
            left join fetch s.empresa
            where s.auditor.id = :auditorId
              and s.estado in :estadosCompletados
            """)
    List<SolicitudAuditoria> listarCompletadasPorAuditor(
            @Param("auditorId") UUID auditorId,
            @Param("estadosCompletados") Collection<EstadoSolicitudAuditoria> estadosCompletados);

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
     * Pagina las solicitudes de una empresa. El {@code sinFiltro} evita el {@code in ()} vacio, que
     * Hibernate traduce a SQL invalido: cuando no hay filtro la condicion se apaga entera en vez de
     * comparar contra una lista sin elementos.
     */
    @Query(value = """
            select s from SolicitudAuditoria s
            left join fetch s.empresa
            left join fetch s.auditor
            where s.empresa.id = :empresaId
              and (:sinFiltro = true or s.estado in :estados)
            """,
            countQuery = """
            select count(s) from SolicitudAuditoria s
            where s.empresa.id = :empresaId
              and (:sinFiltro = true or s.estado in :estados)
            """)
    Page<SolicitudAuditoria> paginarPorEmpresa(@Param("empresaId") UUID empresaId,
                                               @Param("sinFiltro") boolean sinFiltro,
                                               @Param("estados") Collection<EstadoSolicitudAuditoria> estados,
                                               Pageable pageable);

    /**
     * Solicitudes que le corresponden a un auditor: las que tiene asignadas <em>ahora</em> mas las
     * que gestiono alguna vez.
     *
     * <p>La segunda mitad no es un adorno. El campo {@code auditor} solo conserva al vigente, y una
     * solicitud que el auditor acepto y luego se reasigno, rechazo o vencio deja ese campo en otro
     * o en nulo. Sin mirar el historial, esas auditorias desaparecen de su listado justo despues de
     * que las trabajo, y no le queda forma de volver a encontrarlas.</p>
     */
    @Query(value = """
            select s from SolicitudAuditoria s
            left join fetch s.empresa
            left join fetch s.auditor a
            where (a.id = :auditorId
                   or exists (select 1 from TransicionEstadoAuditoria t
                               where t.solicitud.id = s.id
                                 and t.responsableId = :auditorId
                                 and t.estadoNuevo = :estadoAsignado))
              and (:sinFiltro = true or s.estado in :estados)
            """,
            countQuery = """
            select count(s) from SolicitudAuditoria s
            where (s.auditor.id = :auditorId
                   or exists (select 1 from TransicionEstadoAuditoria t
                               where t.solicitud.id = s.id
                                 and t.responsableId = :auditorId
                                 and t.estadoNuevo = :estadoAsignado))
              and (:sinFiltro = true or s.estado in :estados)
            """)
    Page<SolicitudAuditoria> paginarPorAuditor(@Param("auditorId") UUID auditorId,
                                               @Param("estadoAsignado") EstadoSolicitudAuditoria estadoAsignado,
                                               @Param("sinFiltro") boolean sinFiltro,
                                               @Param("estados") Collection<EstadoSolicitudAuditoria> estados,
                                               Pageable pageable);

    @Query("""
            select s.id from SolicitudAuditoria s
            where s.auditor is not null
              and s.estado = :estadoSinRespuesta
              and s.fechaAsignacion < :limite
            """)
    List<UUID> idsConAsignacionVencida(@Param("estadoSinRespuesta") EstadoSolicitudAuditoria estadoSinRespuesta,
                                       @Param("limite") Instant limite);

    @Query("""
            select s
            from SolicitudAuditoria s
            where s.empresa.id = :empresaId
              and s.estado = :estadoVerificado
              and exists (
                  select 1
                  from Certificacion c
                  where c.idAuditoria = s.id
                    and c.estado = :estadoCertificacion
              )
            order by s.periodoInicio asc, s.periodoFin asc
            """)
    List<SolicitudAuditoria> listarPeriodosVerificados(
            @Param("empresaId") UUID empresaId,
            @Param("estadoVerificado") EstadoSolicitudAuditoria estadoVerificado,
            @Param("estadoCertificacion") EstadoCertificacion estadoCertificacion);
}
