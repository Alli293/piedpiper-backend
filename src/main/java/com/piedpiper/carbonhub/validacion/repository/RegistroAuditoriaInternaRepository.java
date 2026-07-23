package com.piedpiper.carbonhub.validacion.repository;

import com.piedpiper.carbonhub.validacion.models.entities.RegistroAuditoriaInterna;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegistroAuditoriaInternaRepository
        extends JpaRepository<RegistroAuditoriaInterna, UUID> {
}
