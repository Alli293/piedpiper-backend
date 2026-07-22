package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmisionRepository extends JpaRepository<Emision, UUID> {

    List<Emision> findAllByEmpresaIdOrderByFechaActividadDescCreatedAtDesc(UUID empresaId);

    List<Emision> findAllByEmpresaIdAndFechaActividadGreaterThanEqualAndFechaActividadLessThanOrderByFechaActividadDescCreatedAtDesc(
            UUID empresaId,
            LocalDate inicio,
            LocalDate fin);

    @Query("""
            select sum(e.carbonKg)
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad >= :inicio
              and e.fechaActividad < :fin
            """)
    BigDecimal sumCarbonKgByEmpresaIdAndFechaActividadEntre(@Param("empresaId") UUID empresaId,
                                                            @Param("inicio") LocalDate inicio,
                                                            @Param("fin") LocalDate fin);

    @Query("""
            select e
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad >= :inicio
              and e.fechaActividad < :fin
            """)
    List<Emision> findAllByEmpresaIdAndPeriodo(@Param("empresaId") UUID empresaId,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fin") LocalDate fin);

    Optional<Emision> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Emision> findAllByEmpresaIdAndFechaActividadBetween(UUID empresaId, LocalDate desde, LocalDate hasta);
}
