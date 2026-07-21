package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PerfilAuditorRepository extends JpaRepository<PerfilAuditor, UUID> {

    @Query("""
            select p from PerfilAuditor p
            join fetch p.auditor u
            where u.rol = :rol
              and u.estado = :estado
              and (:termino is null
                   or lower(concat(u.nombre, ' ', u.apellidos)) like lower(concat('%', :termino, '%')))
            """)
    Page<PerfilAuditor> buscarDirectorio(
            @Param("rol") Rol rol,
            @Param("estado") EstadoUsuario estado,
            @Param("termino") String termino,
            Pageable pageable);
}
