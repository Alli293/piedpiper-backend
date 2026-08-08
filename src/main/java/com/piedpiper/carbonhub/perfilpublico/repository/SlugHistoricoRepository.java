package com.piedpiper.carbonhub.perfilpublico.repository;

import com.piedpiper.carbonhub.perfilpublico.models.entities.SlugHistorico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlugHistoricoRepository extends JpaRepository<SlugHistorico, UUID> {

    Optional<SlugHistorico> findBySlugAnterior(String slugAnterior);
}
