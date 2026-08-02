package com.piedpiper.carbonhub.empresa.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsBySlug(String slug);

    List<Empresa> findBySectorIndustrial(SectorIndustrial sectorIndustrial);

    Page<Empresa> findByEstado(EstadoEmpresa estado, Pageable pageable);

    Optional<Empresa> findBySlugAndEstado(String slug, EstadoEmpresa estado);
}
