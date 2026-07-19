package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmisionRepository extends JpaRepository<Emision, UUID> {

    @Query("""
            select distinct e
            from Emision e
            left join fetch treat(e as EmisionVuelo).legs
            where e.empresaId = :empresaId
            order by e.createdAt desc
            """)
    List<Emision> findAllByEmpresaIdOrderByCreatedAtDesc(@Param("empresaId") UUID empresaId);

    Optional<Emision> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Emision> findAllByEmpresaIdAndFechaActividadBetween(UUID empresaId, LocalDate desde, LocalDate hasta);
}
