package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.reconocimiento.mappers.EventoReconocimientoMapper;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoReconocimientoResponseDTO;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.RegistrarEventoReconocimientoRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EventoReconocimientoCodigo;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventoReconocimientoService {

    private static final Logger log = LoggerFactory.getLogger(EventoReconocimientoService.class);

    private final EventoReconocimientoRepository eventoReconocimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoReconocimientoEnvioService eventoReconocimientoEnvioService;
    private final EventoReconocimientoMapper eventoReconocimientoMapper;

    public EventoReconocimientoService(EventoReconocimientoRepository eventoReconocimientoRepository,
                                       UsuarioRepository usuarioRepository,
                                       EventoReconocimientoEnvioService eventoReconocimientoEnvioService,
                                       EventoReconocimientoMapper eventoReconocimientoMapper) {
        this.eventoReconocimientoRepository = eventoReconocimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoReconocimientoEnvioService = eventoReconocimientoEnvioService;
        this.eventoReconocimientoMapper = eventoReconocimientoMapper;
    }

    @Transactional
    public EventoReconocimientoResponseDTO registrar(
            RegistrarEventoReconocimientoRequestDTO request,
            UUID usuarioAutenticadoId) {
        if (!usuarioAutenticadoId.equals(request.getUsuarioId())) {
            throw ApiException.accesoDenegado(
                    "El evento debe corresponder al usuario autenticado.");
        }
        return generar(usuarioAutenticadoId, request.getEventoGenerado());
    }

    @Transactional
    public EventoReconocimientoResponseDTO generar(UUID usuarioId, String eventoGenerado) {
        validarUsuarioActivo(usuarioId);

        String codigoRecibido = eventoGenerado == null ? "" : eventoGenerado.trim();
        EventoReconocimiento evento = EventoReconocimientoCodigo.desde(codigoRecibido)
                .map(catalogo -> eventoValido(usuarioId, catalogo))
                .orElseGet(() -> eventoFueraCatalogo(usuarioId, codigoRecibido));
        EventoReconocimiento existente = eventoReconocimientoRepository
                .findByUsuarioIdAndEventoGenerado(usuarioId, evento.getEventoGenerado())
                .orElse(null);
        if (existente != null) {
            return eventoReconocimientoMapper.toDto(existente);
        }

        EventoReconocimiento guardado = eventoReconocimientoRepository.save(evento);
        if (guardado.getEstadoEnvio() == EstadoEnvioCertificacion.PENDIENTE_ENVIO) {
            enviarTrasCommit(guardado.getId());
        }
        return eventoReconocimientoMapper.toDto(guardado);
    }

    private EventoReconocimiento eventoValido(UUID usuarioId, EventoReconocimientoCodigo catalogo) {
        return EventoReconocimiento.builder()
                .usuarioId(usuarioId)
                .eventoGenerado(catalogo.getCodigo())
                .fechaEvento(Instant.now())
                .estadoEnvio(EstadoEnvioCertificacion.PENDIENTE_ENVIO)
                .build();
    }

    private EventoReconocimiento eventoFueraCatalogo(UUID usuarioId, String eventoGenerado) {
        log.warn("Evento de reconocimiento fuera de catalogo para usuario {}: {}",
                usuarioId, eventoGenerado);
        return EventoReconocimiento.builder()
                .usuarioId(usuarioId)
                .eventoGenerado(eventoGenerado)
                .fechaEvento(Instant.now())
                .estadoEnvio(EstadoEnvioCertificacion.FUERA_CATALOGO)
                .build();
    }

    private void validarUsuarioActivo(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ApiException.accesoDenegado(
                        "No se pudo identificar al usuario autenticado."));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw ApiException.accesoDenegado(
                    "El usuario autenticado no se encuentra activo.");
        }
    }

    private void enviarTrasCommit(UUID eventoId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventoReconocimientoEnvioService.enviarAsync(eventoId);
                }
            });
        } else {
            eventoReconocimientoEnvioService.enviarAsync(eventoId);
        }
    }
}
