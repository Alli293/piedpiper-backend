package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    @Query("""
            select count(distinct type(e))
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad between :desde and :hasta
            """)
    long contarCategoriasConRegistro(@Param("empresaId") UUID empresaId,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);

    @Query("""
            select count(distinct (extract(year from e.fechaActividad) * 100 + extract(month from e.fechaActividad)))
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad between :desde and :hasta
            """)
    long contarMesesConRegistro(@Param("empresaId") UUID empresaId,
                                @Param("desde") LocalDate desde,
                                @Param("hasta") LocalDate hasta);

    @Query("""
            select coalesce(sum(e.carbonKg), 0)
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad between :desde and :hasta
            """)
    BigDecimal sumarCarbonKgEnVentana(@Param("empresaId") UUID empresaId,
                                      @Param("desde") LocalDate desde,
                                      @Param("hasta") LocalDate hasta);
}
