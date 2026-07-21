package com.piedpiper.carbonhub.auditor.repository;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public interface PerfilAuditorRepository extends JpaRepository<PerfilAuditor, UUID> {

    String FILTROS = """
            where u.rol = :rol
              and u.estado = :estado
              and (:termino is null
                   or lower(concat(coalesce(u.nombre, ''), ' ', coalesce(u.apellidos, '')))
                      like lower(concat('%', :termino, '%')))
              and (:provincia is null or p.provincia = :provincia)
              and (:calificacionMinima is null or p.calificacionPromedio >= :calificacionMinima)
              and (:soloDisponibles = false or p.disponible = true)
              and (:filtrarEspecialidades = false
                   or exists (select esp from p.especialidades esp where esp in :especialidades))
            """;

    @Query(value = "select p from PerfilAuditor p join fetch p.auditor u " + FILTROS,
            countQuery = "select count(p) from PerfilAuditor p join p.auditor u " + FILTROS)
    Page<PerfilAuditor> buscarDirectorio(
            @Param("rol") Rol rol,
            @Param("estado") EstadoUsuario estado,
            @Param("termino") String termino,
            @Param("provincia") ProvinciaCR provincia,
            @Param("calificacionMinima") BigDecimal calificacionMinima,
            @Param("soloDisponibles") boolean soloDisponibles,
            @Param("filtrarEspecialidades") boolean filtrarEspecialidades,
            @Param("especialidades") Collection<EspecialidadAuditor> especialidades,
            Pageable pageable);
}
