package com.piedpiper.carbonhub.validacion.repository;

import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentoCredencialAuditorRepository extends JpaRepository<DocumentoCredencialAuditor, UUID> {

    List<DocumentoCredencialAuditor> findBySolicitudId(UUID solicitudId);
}
