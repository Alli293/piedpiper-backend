package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface PreferenciasViajeRepository extends JpaRepository<PreferenciasViaje, UUID> {

    Optional<PreferenciasViaje> findByUsuario_Id(UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PreferenciasViaje> findWithLockByUsuario_Id(UUID usuarioId);
}
