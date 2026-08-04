package com.piedpiper.carbonhub.empresa.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsBySlug(String slug);

    List<Empresa> findBySectorIndustrial(SectorIndustrial sectorIndustrial);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empresa e where e.id = :empresaId")
    Optional<Empresa> bloquearPorId(UUID empresaId);

    Optional<Empresa> findBySlugAndEstado(String slug, EstadoEmpresa estado);

    Optional<Empresa> findBySlug(String slug);

    Page<Empresa> findByNombreEmpresaContainingIgnoreCaseAndEstado(String nombre, EstadoEmpresa estado, Pageable pageable);

    Page<Empresa> findByEstado(EstadoEmpresa estado, Pageable pageable);

    List<Empresa> findByEstado(EstadoEmpresa estado);
}
