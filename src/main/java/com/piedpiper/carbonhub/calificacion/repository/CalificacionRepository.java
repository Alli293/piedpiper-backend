package com.piedpiper.carbonhub.calificacion.repository;

import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {

    boolean existsByAuditoriaIdAndEmpresaId(UUID auditoriaId, UUID empresaId);

    Optional<Calificacion> findByAuditoriaIdAndEmpresaId(UUID auditoriaId, UUID empresaId);

    // join fetch de empresa: el perfil público arma una reseña por fila con nombreEmpresa, así que
    // sin esto cada reseña dispara una carga perezosa adicional (N+1) para un auditor con varias.
    @Query("""
            select c from Calificacion c
            join fetch c.empresa
            where c.auditor.id = :auditorId
            order by c.creadoEn desc
            """)
    List<Calificacion> findByAuditorIdOrderByCreadoEnDesc(@Param("auditorId") UUID auditorId);
}
