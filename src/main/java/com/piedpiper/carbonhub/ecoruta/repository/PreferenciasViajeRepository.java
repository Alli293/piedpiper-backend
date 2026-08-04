package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PreferenciasViajeRepository extends JpaRepository<PreferenciasViaje, UUID> {

    Optional<PreferenciasViaje> findByUsuario_Id(UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreferenciasViaje p WHERE p.usuario.id = :usuarioId")
    Optional<PreferenciasViaje> findByUsuario_IdForUpdate(@Param("usuarioId") UUID usuarioId);
}
