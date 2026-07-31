package com.piedpiper.carbonhub.empresa.repository;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsBySlug(String slug);

    List<Empresa> findBySectorIndustrial(SectorIndustrial sectorIndustrial);

    /**
     * Toma un lock de escritura sobre la fila de la empresa, para serializar entre si las
     * operaciones que primero consultan y despues insertan en funcion de lo consultado (PP-44).
     * Sin el, dos peticiones simultaneas de la misma empresa pasan las dos la validacion antes de
     * que cualquiera haga commit.
     *
     * <p>Solo compiten entre si las peticiones de la misma empresa: dos empresas distintas bloquean
     * filas distintas y no se estorban.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Empresa e where e.id = :empresaId")
    Optional<Empresa> bloquearPorId(UUID empresaId);
}
