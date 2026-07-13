package com.piedpiper.carbonhub.emision.repository;

import com.piedpiper.carbonhub.emision.models.entities.Emision;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmisionRepository extends JpaRepository<Emision, UUID> {

    Page<Emision> findByEmpresaId(UUID empresaId, Pageable pageable);

    List<Emision> findAllByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId);

    Optional<Emision> findByIdAndCreatedByUserId(UUID id, UUID createdByUserId);
}
