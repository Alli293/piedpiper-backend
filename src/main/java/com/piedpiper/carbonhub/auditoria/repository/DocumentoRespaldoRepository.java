package com.piedpiper.carbonhub.auditoria.repository;

import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentoRespaldoRepository extends JpaRepository<DocumentoRespaldo, UUID> {

    /**
     * Cuenta los adjuntos de varias solicitudes en una sola consulta.
     *
     * <p>Existe para que el listado no cuente sobre la coleccion de la entidad: {@code documentos}
     * es perezosa, asi que un {@code .size()} por fila dispara una consulta extra que materializa
     * los documentos completos, incluido el {@code contenido bytea} de cada PDF. Contar aca trae
     * numeros y nada mas.</p>
     */
    @Query("""
            select d.solicitud.id, count(d)
              from DocumentoRespaldo d
             where d.solicitud.id in :solicitudIds
             group by d.solicitud.id
            """)
    List<Object[]> contarPorSolicitud(@Param("solicitudIds") Collection<UUID> solicitudIds);
}
