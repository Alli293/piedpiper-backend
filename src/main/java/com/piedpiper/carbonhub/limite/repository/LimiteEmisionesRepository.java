package com.piedpiper.carbonhub.limite.repository;

import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimiteEmisionesRepository extends JpaRepository<LimiteEmisiones, Long> {
    Optional<LimiteEmisiones> findByEmpresaIdAndAnio(Long empresaId, Integer anio);

    List<LimiteEmisiones> findAllByEmpresaIdOrderByAnioDesc(Long empresaId);
}
