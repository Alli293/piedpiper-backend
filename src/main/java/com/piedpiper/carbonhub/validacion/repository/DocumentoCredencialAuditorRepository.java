package com.piedpiper.carbonhub.validacion.repository;

import com.piedpiper.carbonhub.validacion.models.dtos.DocumentoCredencialResumenResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentoCredencialAuditorRepository extends JpaRepository<DocumentoCredencialAuditor, UUID> {

    // Proyeccion JPQL en vez de MapStruct (desviacion deliberada de CONVENTIONS.md #4.4): el punto
    // de esta consulta es evitar traer "contenido" (bytea, hasta 15MB por fila) solo para armar un
    // resumen. MapStruct mapea a partir de la entidad ya cargada, asi que no evita ese fetch.
    @Query("select new com.piedpiper.carbonhub.validacion.models.dtos.DocumentoCredencialResumenResponseDTO("
            + "d.id, d.nombreArchivo, d.tamanioBytes) "
            + "from DocumentoCredencialAuditor d where d.solicitud.id = :solicitudId")
    List<DocumentoCredencialResumenResponseDTO> resumenPorSolicitudId(@Param("solicitudId") UUID solicitudId);
}
