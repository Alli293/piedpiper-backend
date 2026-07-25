package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreferenciasViajeRepository extends JpaRepository<PreferenciasViaje, UUID> {

    Optional<PreferenciasViaje> findByUsuario_Id(UUID usuarioId);
}
