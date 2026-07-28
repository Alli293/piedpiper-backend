package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.IndiceEstadoCertificacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IndiceEstadoCertificacionRepository
        extends JpaRepository<IndiceEstadoCertificacion, Long> {
}
