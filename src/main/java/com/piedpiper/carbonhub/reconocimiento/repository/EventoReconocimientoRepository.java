package com.piedpiper.carbonhub.reconocimiento.repository;

import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoReconocimientoRepository extends JpaRepository<EventoReconocimiento, UUID> {

    List<EventoReconocimiento> findTop50ByEstadoEnvioOrderByFechaEventoAsc(
            EstadoEnvioCertificacion estadoEnvio);

    Optional<EventoReconocimiento> findByUsuarioIdAndEventoGenerado(UUID usuarioId, String eventoGenerado);
}
