package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventoReconocimientoEnvioService {

    private final EventoReconocimientoRepository eventoReconocimientoRepository;
    private final EventoReconocimientoIntentoEnvioService eventoReconocimientoIntentoEnvioService;

    public EventoReconocimientoEnvioService(
            EventoReconocimientoRepository eventoReconocimientoRepository,
            EventoReconocimientoIntentoEnvioService eventoReconocimientoIntentoEnvioService) {
        this.eventoReconocimientoRepository = eventoReconocimientoRepository;
        this.eventoReconocimientoIntentoEnvioService = eventoReconocimientoIntentoEnvioService;
    }

    @Async
    public void enviarAsync(UUID eventoId) {
        eventoReconocimientoIntentoEnvioService.enviar(eventoId);
    }

    @Scheduled(fixedDelayString = "${certificacion.reintento-intervalo-ms:300000}")
    public void reenviarPendientes() {
        eventoReconocimientoRepository
                .findTop50ByEstadoEnvioOrderByFechaEventoAsc(EstadoEnvioCertificacion.PENDIENTE_REINTENTO)
                .forEach(evento -> eventoReconocimientoIntentoEnvioService.enviar(evento.getId()));
    }
}
