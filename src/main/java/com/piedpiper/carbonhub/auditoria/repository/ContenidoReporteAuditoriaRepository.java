package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.ContenidoReporteAuditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContenidoReporteAuditoriaRepository extends JpaRepository<ContenidoReporteAuditoria, UUID> {

    long deleteByReporteAuditoriaId(UUID reporteAuditoriaId);
}
