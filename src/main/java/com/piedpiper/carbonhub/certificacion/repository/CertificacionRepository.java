package com.piedpiper.carbonhub.certificacion.repository;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificacionRepository extends JpaRepository<Certificacion, UUID> {

    Optional<Certificacion> findByIdAuditoria(UUID idAuditoria);

    List<Certificacion> findByEmpresaIdOrderByFechaEmisionDesc(UUID empresaId);

    Optional<Certificacion> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
