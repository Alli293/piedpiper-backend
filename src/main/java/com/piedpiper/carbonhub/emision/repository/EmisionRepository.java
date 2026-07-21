package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select sum(e.carbonKg)
            from Emision e
            where e.empresaId = :empresaId
              and year(e.fechaActividad) = :anio
            """)
    BigDecimal sumCarbonKgByEmpresaIdAndAnio(@Param("empresaId") UUID empresaId,
                                             @Param("anio") Integer anio);

    Optional<Emision> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
