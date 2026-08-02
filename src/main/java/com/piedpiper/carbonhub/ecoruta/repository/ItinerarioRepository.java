package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItinerarioRepository extends JpaRepository<Itinerario, UUID> {

    long countByUsuario_Id(UUID usuarioId);

    Optional<Itinerario> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    @Query("""
            select distinct a.provincia
            from Itinerario i join i.dias d join d.actividades a
            where i.usuario.id = :usuarioId
            """)
    List<Provincia> findProvinciasVisitadasByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
