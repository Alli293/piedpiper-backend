package com.piedpiper.carbonhub.ecoruta.repository;

import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsigniaUsuarioRepository extends JpaRepository<InsigniaUsuario, UUID> {

    boolean existsByUsuarioIdAndIdInsignia(UUID usuarioId, Long idInsignia);

    List<InsigniaUsuario> findByUsuarioIdOrderByFechaObtencionDesc(UUID usuarioId);
}
