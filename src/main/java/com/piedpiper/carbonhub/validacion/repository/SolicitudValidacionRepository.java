package com.piedpiper.carbonhub.validacion.repository;

import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SolicitudValidacionRepository extends JpaRepository<SolicitudValidacion, UUID> {

    @EntityGraph(attributePaths = "auditor")
    Page<SolicitudValidacion> findAllByEstadoOrderByFechaSolicitudAsc(
            EstadoSolicitud estado, Pageable pageable);

    Optional<SolicitudValidacion> findTopByAuditorIdOrderByFechaSolicitudDesc(UUID auditorId);

    // Sobreescribe JpaRepository.findById: todos los usos actuales (obtenerDetalle, resolver)
    // necesitan el auditor para el nombre/email en la respuesta o el correo de resolucion.
    @EntityGraph(attributePaths = "auditor")
    Optional<SolicitudValidacion> findById(UUID id);
}
