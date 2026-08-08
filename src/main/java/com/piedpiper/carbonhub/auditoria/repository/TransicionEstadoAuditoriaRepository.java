package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransicionEstadoAuditoriaRepository extends JpaRepository<TransicionEstadoAuditoria, UUID> {

    List<TransicionEstadoAuditoria> findBySolicitudIdOrderByFechaAsc(UUID solicitudId);

    /**
     * Ids de los auditores que en algun momento estuvieron asignados a la solicitud. La historia
     * pide que un auditor que ya respondio siga viendo el detalle, y el campo {@code auditor} de la
     * solicitud solo conserva al vigente: tras un rechazo o un vencimiento queda nulo. El historial
     * es el unico lugar donde ese vinculo sobrevive.
     */
    @Query("""
            select distinct t.responsableId from TransicionEstadoAuditoria t
            where t.solicitud.id = :solicitudId
              and t.actor = com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria.AUDITOR
              and t.responsableId is not null
            """)
    List<UUID> idsAuditoresConHistorial(@Param("solicitudId") UUID solicitudId);
}
