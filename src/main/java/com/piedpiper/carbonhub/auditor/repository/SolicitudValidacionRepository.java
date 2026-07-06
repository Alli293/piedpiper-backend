package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.SolicitudValidacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitudValidacionRepository extends JpaRepository<SolicitudValidacion, UUID> {
}
