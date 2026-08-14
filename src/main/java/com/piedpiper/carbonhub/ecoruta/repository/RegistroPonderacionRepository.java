package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.RegistroPonderacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroPonderacionRepository extends JpaRepository<RegistroPonderacion, UUID> {

    List<RegistroPonderacion> findByItinerarioId(UUID itinerarioId);
}
