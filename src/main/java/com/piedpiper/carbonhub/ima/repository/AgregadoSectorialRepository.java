package com.piedpiper.carbonhub.ima.repository;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgregadoSectorialRepository extends JpaRepository<AgregadoSectorial, UUID> {

    Optional<AgregadoSectorial> findBySectorAndAnioAndMes(SectorIndustrial sector, Integer anio, Integer mes);

    /**
     * Agregados del sector dentro de una ventana [desde, hasta] expresada como (anio, mes).
     * La tendencia deriva de aquí el conteo de empresas elegibles, de modo que el umbral
     * de privacidad sea el mismo que aplica {@code ImaService} en /api/ima y /api/ima/benchmark.
     */
    @Query("""
            SELECT a FROM AgregadoSectorial a
            WHERE a.sector = :sector
              AND (a.anio * 12 + a.mes) BETWEEN (:anioDesde * 12 + :mesDesde) AND (:anioHasta * 12 + :mesHasta)
            """)
    List<AgregadoSectorial> findVentana(@Param("sector") SectorIndustrial sector,
                                        @Param("anioDesde") int anioDesde,
                                        @Param("mesDesde") int mesDesde,
                                        @Param("anioHasta") int anioHasta,
                                        @Param("mesHasta") int mesHasta);
}
