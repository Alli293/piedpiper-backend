package com.piedpiper.carbonhub.ima.repository;

import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImaSnapshotRepository extends JpaRepository<ImaSnapshot, UUID> {

    Optional<ImaSnapshot> findByEmpresaIdAndAnioAndMes(UUID empresaId, Integer anio, Integer mes);

    Optional<ImaSnapshot> findFirstByEmpresaIdOrderByAnioDescMesDesc(UUID empresaId);

    /**
     * Último snapshot por cada empresa de {@code empresaIds}, en una sola consulta.
     * Evita el N+1 de resolver el nivel ecológico empresa por empresa en listados paginados.
     */
    @Query("""
            SELECT s FROM ImaSnapshot s
            WHERE s.empresaId IN :empresaIds
              AND (s.anio * 12 + s.mes) = (
                  SELECT MAX(s2.anio * 12 + s2.mes) FROM ImaSnapshot s2 WHERE s2.empresaId = s.empresaId
              )
            """)
    List<ImaSnapshot> findUltimosPorEmpresaIds(@Param("empresaIds") Collection<UUID> empresaIds);

    List<ImaSnapshot> findByInterpretacion(String interpretacion);

    void deleteAllByEmpresaId(UUID empresaId);

    void deleteAllByEmpresaIdIn(Collection<UUID> empresaIds);

    /**
     * Snapshots de la empresa dentro de una ventana [desde, hasta] expresada como (anio, mes).
     * El orden por (anio, mes) permite recorrer la serie cronológicamente sin reordenar en memoria.
     */
    @Query("""
            SELECT s FROM ImaSnapshot s
            WHERE s.empresaId = :empresaId
              AND (s.anio * 12 + s.mes) BETWEEN (:anioDesde * 12 + :mesDesde) AND (:anioHasta * 12 + :mesHasta)
            ORDER BY s.anio ASC, s.mes ASC
            """)
    List<ImaSnapshot> findVentana(@Param("empresaId") UUID empresaId,
                                  @Param("anioDesde") int anioDesde,
                                  @Param("mesDesde") int mesDesde,
                                  @Param("anioHasta") int anioHasta,
                                  @Param("mesHasta") int mesHasta);

}
