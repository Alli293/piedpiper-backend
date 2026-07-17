package com.piedpiper.carbonhub.ima.repository;

import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImaSnapshotRepository extends JpaRepository<ImaSnapshot, UUID> {

    Optional<ImaSnapshot> findByEmpresaIdAndAnioAndMes(UUID empresaId, Integer anio, Integer mes);
}
