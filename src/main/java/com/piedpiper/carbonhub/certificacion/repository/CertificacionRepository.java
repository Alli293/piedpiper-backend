package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificacionRepository extends JpaRepository<Certificacion, UUID> {

    Optional<Certificacion> findByIdAuditoria(UUID idAuditoria);

    /**
     * Usada por el proceso nocturno de alertas de vencimiento (PP-70): evalua
     * todas las certificaciones activas de todas las empresas, sin importar
     * quien esta autenticado.
     */
    List<Certificacion> findByEstado(EstadoCertificacion estado);

    List<Certificacion> findByEmpresaIdOrderByFechaEmisionDesc(UUID empresaId);

    Optional<Certificacion> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Certificacion> findByEmpresaIdAndEstadoOrderByFechaEmisionDesc(
            UUID empresaId, EstadoCertificacion estado);

    /**
     * {@code fechaVencimiento} estrictamente posterior a {@code hoy}: coincide
     * con la semantica de {@code exp} en el VC-JWT, que vence a medianoche UTC
     * del dia de {@code fechaVencimiento} (ver GeneradorCredencialOpenBadges).
     * Una certificacion no vigente no debe aparecer en el perfil publico aunque
     * su {@code estado} siga siendo ACTIVA (activa = no revocada; vigente = no
     * vencida, son conceptos distintos).
     */
    List<Certificacion> findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
            UUID empresaId, EstadoCertificacion estado, LocalDate hoy);

    long countByEmpresaIdAndEstado(UUID empresaId, EstadoCertificacion estado);

    @Query("""
            select count(distinct c.tipo)
            from Certificacion c
            where c.empresa.id = :empresaId
              and c.estado = :estado
              and c.tipo in :tipos
            """)
    long countDistinctTiposActivos(
            @Param("empresaId") UUID empresaId,
            @Param("estado") EstadoCertificacion estado,
            @Param("tipos") Collection<TipoCertificacion> tipos);
}
