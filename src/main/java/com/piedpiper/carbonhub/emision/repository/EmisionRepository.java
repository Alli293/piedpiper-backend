package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    Optional<Emision> findByIdAndEmpresaId(UUID id, UUID empresaId);

    @Query("""
            select extract(month from e.fechaActividad), coalesce(sum(e.carbonKg), 0)
            from Emision e
            where e.empresaId = :empresaId
              and extract(year from e.fechaActividad) = :anio
            group by extract(month from e.fechaActividad)
            """)
    List<Object[]> sumarCarbonKgPorMes(@Param("empresaId") UUID empresaId,
                                       @Param("anio") int anio);
}
