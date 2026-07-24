package com.piedpiper.carbonhub.ima.repository;

import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.ima.models.entities.AgregadoSectorial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgregadoSectorialRepository extends JpaRepository<AgregadoSectorial, UUID> {

    Optional<AgregadoSectorial> findBySectorAndAnioAndMes(SectorIndustrial sector, Integer anio, Integer mes);
}
