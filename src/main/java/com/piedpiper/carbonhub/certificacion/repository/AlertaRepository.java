package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Alerta;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoAlerta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    boolean existsByCertificacionIdAndTipoAlerta(UUID certificacionId, TipoAlerta tipoAlerta);

    /**
     * Usada por el resumen de certificaciones del dashboard (PP-74) para saber
     * que certificaciones ya cruzaron un umbral de vencimiento (90/30/7 dias).
     */
    List<Alerta> findByEmpresaId(UUID empresaId);
}
