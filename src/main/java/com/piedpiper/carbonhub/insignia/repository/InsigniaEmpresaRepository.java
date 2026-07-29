package com.piedpiper.carbonhub.insignia.repository;

import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsigniaEmpresaRepository extends JpaRepository<InsigniaEmpresa, UUID> {

    boolean existsByEmpresaIdAndIdInsigniaAndNivelInsignia(
            UUID empresaId, Long idInsignia, String nivelInsignia);

    List<InsigniaEmpresa> findByEmpresaIdOrderByFechaObtencionDesc(UUID empresaId);
}
