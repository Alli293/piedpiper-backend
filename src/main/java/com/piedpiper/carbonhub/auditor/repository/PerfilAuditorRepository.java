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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerfilAuditorRepository extends JpaRepository<PerfilAuditor, UUID> {

    Optional<PerfilAuditor> findByAuditorId(UUID auditorId);

    @Query("""
            select distinct p from PerfilAuditor p
            left join fetch p.distribucionSectores
            where p.auditor.id = :auditorId
            """)
    Optional<PerfilAuditor> findByAuditorIdConDistribucion(@Param("auditorId") UUID auditorId);

    @Query("""
            select distinct p from PerfilAuditor p
            join fetch p.auditor
            left join fetch p.distribucionSectores
            where p.auditor.id = :auditorId
              and p.auditor.estado = :estado
            """)
    Optional<PerfilAuditor> findByAuditorIdAndAuditorEstadoConDistribucion(
            @Param("auditorId") UUID auditorId,
            @Param("estado") EstadoUsuario estado);

    @Query("""
            select p.auditor.id from PerfilAuditor p
            """)
    List<UUID> listarAuditorIdsConPerfil();

    boolean existsByAuditorId(UUID auditorId);

    /**
     * Candidatos para la recomendación (PP-57): auditores certificados y activos que tienen la
     * especialidad buscada y cubren la zona pedida. {@code member of} sobre las colecciones del
     * perfil hace el filtro dentro de la consulta, sin traer a memoria a quien no califica.
     *
     * <p>Se hace {@code join fetch} solo del usuario. Las especialidades y la distribución por
     * sector se recorren después, dentro de la misma transacción de solo lectura, para ordenar los
     * candidatos; son colecciones de enum/valores pequeñas y el conjunto ya viene acotado por los
     * filtros, así que no vale la pena el producto cartesiano de fetch-joinear dos colecciones.</p>
     */
    @Query("""
            select p from PerfilAuditor p
            join fetch p.auditor u
            where u.rol = :rol
              and u.estado = :estado
              and :especialidad member of p.especialidades
              and :zona member of p.zonasCobertura
              and (:soloDisponibles = false or p.disponible = true)
            """)
    List<PerfilAuditor> buscarCandidatosRecomendacion(
            @Param("rol") Rol rol,
            @Param("estado") EstadoUsuario estado,
            @Param("especialidad") EspecialidadAuditor especialidad,
            @Param("zona") ProvinciaCR zona,
            @Param("soloDisponibles") boolean soloDisponibles);

    // El cast de :termino es necesario, no es un no-op: al venir null sin tipo dentro de un
    // concat(), Postgres lo infiere como bytea y falla porque lower(bytea) no existe.
    String FILTROS = """
            where u.rol = :rol
              and u.estado = :estado
              and (cast(:termino as string) is null
                   or lower(concat(coalesce(u.nombre, ''), ' ', coalesce(u.apellidos, '')))
                      like lower(concat('%', cast(:termino as string), '%')))
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
