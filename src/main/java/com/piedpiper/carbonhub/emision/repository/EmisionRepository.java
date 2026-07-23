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

    @Query("""
            select distinct e
            from Emision e
            left join fetch treat(e as EmisionVuelo).legs
            where e.empresaId = :empresaId
              and (:categoria is null
                   or (:categoria = com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.ELECTRICIDAD and type(e) = EmisionElectricidad)
                   or (:categoria = com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.FLOTA and type(e) = EmisionFlota)
                   or (:categoria = com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.VUELO and type(e) = EmisionVuelo)
                   or (:categoria = com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.ENVIO and type(e) = EmisionEnvio))
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                 @Param("categoria") CategoriaEmision categoria,
                                                 @Param("anio") Integer anio,
                                                 @Param("mes") Integer mes);

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
    List<Emision> findAllByEmpresaIdAndFechaActividadBetween(UUID empresaId, LocalDate desde, LocalDate hasta);
}
