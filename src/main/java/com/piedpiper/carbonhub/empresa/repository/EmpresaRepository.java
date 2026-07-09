package com.piedpiper.carbonhub.empresa.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsByCorreoCorporativo(String correoCorporativo);

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsBySlug(String slug);

    Optional<Empresa> findByCorreoCorporativo(String correoCorporativo);
}
