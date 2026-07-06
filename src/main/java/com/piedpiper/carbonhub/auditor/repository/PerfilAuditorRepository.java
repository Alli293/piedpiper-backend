package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerfilAuditorRepository extends JpaRepository<PerfilAuditor, UUID> {
}
