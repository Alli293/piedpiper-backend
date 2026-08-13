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

    @Query("""
            select distinct a.provincia
            from Itinerario i join i.dias d join d.actividades a
            where i.usuario.id = :usuarioId
            """)
    List<Provincia> findProvinciasVisitadasByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Provincias distinct de un único itinerario — usado por el listado (PP-89) para armar el
     * título de cada tarjeta sin traer las actividades completas.
     */
    @Query("""
            select distinct a.provincia
            from Itinerario i join i.dias d join d.actividades a
            where i.id = :itinerarioId
            """)
    List<Provincia> findProvinciasVisitadasByItinerarioId(@Param("itinerarioId") UUID itinerarioId);
}
