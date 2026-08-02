package com.piedpiper.carbonhub.insignia.repository;

import com.piedpiper.carbonhub.insignia.models.entities.CatalogoInsignia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogoInsigniaRepository extends JpaRepository<CatalogoInsignia, Long> {

    List<CatalogoInsignia> findByActivaTrue();

    Optional<CatalogoInsignia> findByIdInsigniaAndNivelInsignia(
            Long idInsignia, String nivelInsignia);

    Optional<CatalogoInsignia> findByIdInsigniaAndNivelInsigniaAndActivaTrue(
            Long idInsignia, String nivelInsignia);
}
