package com.piedpiper.carbonhub.ima.repository;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImaSnapshotRepository extends JpaRepository<ImaSnapshot, UUID> {

    Optional<ImaSnapshot> findByEmpresaIdAndAnioAndMes(UUID empresaId, Integer anio, Integer mes);

    void deleteAllByEmpresaId(UUID empresaId);

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

    /**
     * Promedio del IMA de todas las empresas del sector, mes a mes, dentro de la ventana.
     * Se agrupa por período y se devuelve también el conteo para que el servicio aplique
     * el umbral mínimo de empresas; agrupar aquí evita una consulta por mes.
     */
    @Query("""
            SELECT s.anio AS anio, s.mes AS mes,
                   AVG(s.ima) AS promedioIma, COUNT(DISTINCT s.empresaId) AS cantidadEmpresas
            FROM ImaSnapshot s, Empresa e
            WHERE e.id = s.empresaId
              AND e.sectorIndustrial = :sector
              AND (s.anio * 12 + s.mes) BETWEEN (:anioDesde * 12 + :mesDesde) AND (:anioHasta * 12 + :mesHasta)
            GROUP BY s.anio, s.mes
            """)
    List<PromedioSectorialMensual> promediarImaPorSector(@Param("sector") SectorIndustrial sector,
                                                         @Param("anioDesde") int anioDesde,
                                                         @Param("mesDesde") int mesDesde,
                                                         @Param("anioHasta") int anioHasta,
                                                         @Param("mesHasta") int mesHasta);

    /** Proyección del promedio sectorial de IMA para un período concreto. */
    interface PromedioSectorialMensual {
        Integer getAnio();

        Integer getMes();

        BigDecimal getPromedioIma();

        long getCantidadEmpresas();
    }
}
