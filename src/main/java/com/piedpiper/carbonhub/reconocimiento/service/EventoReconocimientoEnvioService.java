package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EventoReconocimientoEnvioService {

    private static final Logger log = LoggerFactory.getLogger(EventoReconocimientoEnvioService.class);

    private final EventoReconocimientoRepository eventoReconocimientoRepository;
    private final CertificacionEventosClient certificacionEventosClient;

    public EventoReconocimientoEnvioService(EventoReconocimientoRepository eventoReconocimientoRepository,
                                            CertificacionEventosClient certificacionEventosClient) {
        this.eventoReconocimientoRepository = eventoReconocimientoRepository;
        this.certificacionEventosClient = certificacionEventosClient;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarAsync(UUID eventoId) {
        enviarInterno(eventoId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar(UUID eventoId) {
        enviarInterno(eventoId);
    }

    private void enviarInterno(UUID eventoId) {
        EventoReconocimiento evento = eventoReconocimientoRepository.findById(eventoId).orElse(null);
        if (evento == null) {
            log.warn("No se encontro el evento de reconocimiento {} para enviar a Certificacion", eventoId);
            return;
        }
        if (evento.getEstadoEnvio() == EstadoEnvioCertificacion.ENVIADO
                || evento.getEstadoEnvio() == EstadoEnvioCertificacion.FUERA_CATALOGO) {
            return;
        }

        Instant fechaIntento = Instant.now();
        try {
            certificacionEventosClient.enviar(new EventoCertificacionRequestDTO(
                    evento.getUsuarioId(), evento.getEventoGenerado(), evento.getFechaEvento()));
            evento.marcarEnviado(fechaIntento);
            eventoReconocimientoRepository.saveAndFlush(evento);
        } catch (CertificacionNoDisponibleException e) {
            evento.marcarPendienteReintento(e.getMessage(), fechaIntento);
            eventoReconocimientoRepository.saveAndFlush(evento);
            log.warn("Evento {} quedo pendiente para reintento por fallo de Certificacion",
                    evento.getId(), e);
        } catch (RuntimeException e) {
            evento.marcarPendienteReintento("No se pudo enviar el evento a Certificacion.", fechaIntento);
            eventoReconocimientoRepository.saveAndFlush(evento);
            log.warn("Evento {} quedo pendiente para reintento por error inesperado al enviar a Certificacion",
                    evento.getId(), e);
        }
    }

    @Scheduled(fixedDelayString = "${certificacion.reintento-intervalo-ms:300000}")
    @Transactional
    public void reenviarPendientes() {
        List<EventoReconocimiento> pendientes = eventoReconocimientoRepository
                .findTop50ByEstadoEnvioOrderByFechaEventoAsc(EstadoEnvioCertificacion.PENDIENTE_REINTENTO);
        pendientes.forEach(evento -> enviar(evento.getId()));
    }
}
