package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoAlerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    boolean existsByCertificacionIdAndTipoAlerta(UUID certificacionId, TipoAlerta tipoAlerta);

    /**
     * Trae la alerta con su empresa y su certificacion en la misma consulta. El fetch join es
     * necesario porque las dos relaciones son LAZY y el correo se arma fuera de la transaccion
     * que la leyo (PP-71).
     */
    @Query("""
            select a from Alerta a
            join fetch a.empresa
            join fetch a.certificacion
            where a.id = :alertaId
            """)
    Optional<Alerta> buscarConEmpresaYCertificacion(UUID alertaId);

    /**
     * Alertas cuyo correo todavia no salio y que no agotaron los reintentos, para el barrido de
     * recuperacion. Las mas viejas primero.
     */
    List<Alerta> findByEstadoAndIntentosEnvioLessThanOrderByFechaGeneracionAsc(EstadoAlerta estado,
                                                                              int intentosMaximos);
}
