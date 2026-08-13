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

    List<Calificacion> findByAuditorIdOrderByCreadoEnDesc(UUID auditorId);

    @Query("SELECT AVG(c.calificacion) FROM Calificacion c WHERE c.auditor.id = :auditorId")
    Optional<Double> promedioByAuditorId(@Param("auditorId") UUID auditorId);
}
