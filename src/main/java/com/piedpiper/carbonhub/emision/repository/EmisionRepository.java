package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmisionRepository extends JpaRepository<Emision, UUID> {

    boolean existsByEmpresaIdAndFechaActividadLessThanEqual(UUID empresaId, LocalDate fecha);

    @Query("""
            select distinct e
            from Emision e
            left join fetch treat(e as EmisionVuelo).legs
            where e.empresaId = :empresaId
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                @Param("anio") Integer anio,
                                                @Param("mes") Integer mes);

    @Query("""
            select e
            from Emision e
            where e.empresaId = :empresaId
              and type(e) = EmisionElectricidad
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllElectricidadByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                            @Param("anio") Integer anio,
                                                            @Param("mes") Integer mes);

    @Query("""
            select e
            from Emision e
            where e.empresaId = :empresaId
              and type(e) = EmisionFlota
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllFlotaByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                     @Param("anio") Integer anio,
                                                     @Param("mes") Integer mes);

    @Query("""
            select distinct e
            from Emision e
            left join fetch treat(e as EmisionVuelo).legs
            where e.empresaId = :empresaId
              and type(e) = EmisionVuelo
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllVueloByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                     @Param("anio") Integer anio,
                                                     @Param("mes") Integer mes);

    @Query("""
            select e
            from Emision e
            where e.empresaId = :empresaId
              and type(e) = EmisionEnvio
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllEnvioByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
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
            select month(e.fechaActividad), sum(e.carbonKg)
            from Emision e
            where e.empresaId = :empresaId
              and year(e.fechaActividad) = :anio
            group by month(e.fechaActividad)
            """)
    List<Object[]> sumarCarbonKgPorMes(@Param("empresaId") UUID empresaId,
                                       @Param("anio") int anio);

    @Query("""
            select year(e.fechaActividad), sum(e.carbonKg)
            from Emision e
            where e.empresaId = :empresaId
            group by year(e.fechaActividad)
            order by year(e.fechaActividad) asc
            """)
    List<Object[]> sumarCarbonKgPorAnio(@Param("empresaId") UUID empresaId);

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

    /**
     * Categorías distintas registradas por la empresa en cada mes, desde :desde en adelante.
     * Se usa para detectar meses sin registros y la primera aparición de cada categoría.
     * La categoría se resuelve con un CASE sobre type(e) porque la jerarquía es SINGLE_TABLE
     * y el enum no está mapeado como columna propia. Cada subtipo se compara de forma
     * explícita (incluido EmisionEnvio): si se agrega un quinto subtipo sin actualizar esta
     * consulta, la fila queda con categoria null y no se clasifica silenciosamente como ENVIO.
     */
    @Query("""
            select distinct year(e.fechaActividad) as anio,
                   month(e.fechaActividad) as mes,
                   case
                       when type(e) = EmisionElectricidad then com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.ELECTRICIDAD
                       when type(e) = EmisionFlota then com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.FLOTA
                       when type(e) = EmisionVuelo then com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.VUELO
                       when type(e) = EmisionEnvio then com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision.ENVIO
                   end as categoria
            from Emision e
            where e.empresaId = :empresaId
              and e.fechaActividad >= :desde
            """)
    List<CategoriaMensual> listarCategoriasPorMes(@Param("empresaId") UUID empresaId,
                                                  @Param("desde") LocalDate desde);

    /** Proyección de una categoría registrada en un período concreto. */
    interface CategoriaMensual {
        Integer getAnio();

        Integer getMes();

        CategoriaEmision getCategoria();
    }
}