package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
