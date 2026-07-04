package com.piedpiper.carbonhub.empresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByCorreoCorporativoIgnoreCase(String correoCorporativo);
}
