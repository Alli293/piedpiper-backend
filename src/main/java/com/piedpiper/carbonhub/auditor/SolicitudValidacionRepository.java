package com.piedpiper.carbonhub.auditor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitudValidacionRepository extends JpaRepository<SolicitudValidacion, UUID> {
}
