package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

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
                   or (:categoria = 'ELECTRICIDAD' and type(e) = EmisionElectricidad)
                   or (:categoria = 'FLOTA' and type(e) = EmisionFlota)
                   or (:categoria = 'VUELO' and type(e) = EmisionVuelo)
                   or (:categoria = 'ENVIO' and type(e) = EmisionEnvio))
              and (:anio is null or year(e.fechaActividad) = :anio)
              and (:mes is null or month(e.fechaActividad) = :mes)
            order by e.fechaActividad desc, e.createdAt desc
            """)
    List<Emision> findAllByEmpresaIdWithFilters(@Param("empresaId") UUID empresaId,
                                                 @Param("categoria") String categoria,
                                                 @Param("anio") Integer anio,
                                                 @Param("mes") Integer mes);

    Optional<Emision> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
