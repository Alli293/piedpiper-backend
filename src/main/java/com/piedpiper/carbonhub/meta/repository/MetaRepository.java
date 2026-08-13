package com.piedpiper.carbonhub.meta.repository;

import com.piedpiper.carbonhub.meta.models.entities.Meta;
import com.piedpiper.carbonhub.meta.models.enums.EstadoMeta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    List<Meta> findByEmpresaIdAndEstadoOrderByFechaCreacionDesc(UUID empresaId, EstadoMeta estado);

    Optional<Meta> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
