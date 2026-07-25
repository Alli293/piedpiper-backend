package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.NotificacionPanel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacionPanelRepository extends JpaRepository<NotificacionPanel, UUID> {
}
