package com.piedpiper.carbonhub.empresa.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsBySlug(String slug);
}
