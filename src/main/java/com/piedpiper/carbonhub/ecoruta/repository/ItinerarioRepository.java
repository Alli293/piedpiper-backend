package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItinerarioRepository extends JpaRepository<Itinerario, UUID> {

    long countByUsuario_Id(UUID usuarioId);

    Optional<Itinerario> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    /** Listado paginado de "Mis itinerarios" (PP-89). */
    Page<Itinerario> findByUsuario_Id(UUID usuarioId, Pageable pageable);

    Page<Itinerario> findByUsuario_IdAndFavorito(UUID usuarioId, boolean favorito, Pageable pageable);

    @Query("""
            select distinct a.provincia
            from Itinerario i join i.dias d join d.actividades a
            where i.usuario.id = :usuarioId
            """)
    List<Provincia> findProvinciasVisitadasByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Provincias distinct de un conjunto de itinerarios, en una sola consulta — usada por el
     * listado (PP-89) para armar el título de cada tarjeta sin traer las actividades completas.
     * Reemplaza una versión anterior de un solo itinerario que el listado llamaba una vez por
     * fila (N+1 con página fija de 12, señalado en revisión).
     */
    @Query("""
            select i.id as itinerarioId, a.provincia as provincia
            from Itinerario i join i.dias d join d.actividades a
            where i.id in :itinerarioIds
            group by i.id, a.provincia
            """)
    List<ProvinciaPorItinerario> findProvinciasVisitadasPorItinerarios(
            @Param("itinerarioIds") List<UUID> itinerarioIds);

    /** Proyección de {@link #findProvinciasVisitadasPorItinerarios}. */
    interface ProvinciaPorItinerario {
        UUID getItinerarioId();
        Provincia getProvincia();
    }
}
