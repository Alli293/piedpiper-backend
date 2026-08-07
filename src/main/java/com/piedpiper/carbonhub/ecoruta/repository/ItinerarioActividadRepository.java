package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItinerarioActividadRepository extends JpaRepository<ItinerarioActividad, UUID> {

    Optional<ItinerarioActividad> findByIdAndItinerarioDia_Itinerario_Id(UUID actividadId, UUID itinerarioId);
}
