package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventoReconocimientoIntentoEnvioService {

    private static final Logger log = LoggerFactory.getLogger(EventoReconocimientoIntentoEnvioService.class);
    private static final int MAX_REINTENTOS = 3;

    private final EventoReconocimientoRepository eventoReconocimientoRepository;
    private final CertificacionEventosClient certificacionEventosClient;

    public EventoReconocimientoIntentoEnvioService(
            EventoReconocimientoRepository eventoReconocimientoRepository,
            CertificacionEventosClient certificacionEventosClient) {
        this.eventoReconocimientoRepository = eventoReconocimientoRepository;
        this.certificacionEventosClient = certificacionEventosClient;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar(UUID eventoId) {
        EventoReconocimiento evento = eventoReconocimientoRepository.findById(eventoId).orElse(null);
        if (evento == null) {
            log.warn("No se encontro el evento de reconocimiento {} para enviar a Certificacion", eventoId);
            return;
        }
        if (evento.getEstadoEnvio() == EstadoEnvioCertificacion.ENVIADO
                || evento.getEstadoEnvio() == EstadoEnvioCertificacion.FUERA_CATALOGO
                || evento.getEstadoEnvio() == EstadoEnvioCertificacion.REINTENTOS_AGOTADOS) {
            return;
        }

        Instant fechaIntento = Instant.now();
        try {
            certificacionEventosClient.enviar(new EventoCertificacionRequestDTO(
                    evento.getUsuarioId(), evento.getEventoGenerado(), evento.getFechaEvento()));
            evento.marcarEnviado(fechaIntento);
            eventoReconocimientoRepository.saveAndFlush(evento);
        } catch (CertificacionNoDisponibleException e) {
            marcarFallo(evento, e.getMessage(), fechaIntento);
            log.warn("Evento {} quedo pendiente para reintento por fallo de Certificacion",
                    evento.getId(), e);
        } catch (RuntimeException e) {
            marcarFallo(evento, "No se pudo enviar el evento a Certificacion.", fechaIntento);
            log.warn("Evento {} quedo pendiente para reintento por error inesperado al enviar a Certificacion",
                    evento.getId(), e);
        }
    }

    private void marcarFallo(EventoReconocimiento evento, String mensajeError, Instant fechaIntento) {
        if (evento.getIntentosEnvio() + 1 >= MAX_REINTENTOS) {
            evento.marcarReintentosAgotados(mensajeError, fechaIntento);
        } else {
            evento.marcarPendienteReintento(mensajeError, fechaIntento);
        }
        eventoReconocimientoRepository.saveAndFlush(evento);
    }
}
