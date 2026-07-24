package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoReconocimientoEnvioServiceTest {

    @Mock
    private EventoReconocimientoRepository eventoReconocimientoRepository;
    @Mock
    private CertificacionEventosClient certificacionEventosClient;

    @InjectMocks
    private EventoReconocimientoEnvioService service;

    private static final UUID EVENTO_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Test
    void envioCorrecto_marcaEventoComoEnviado() {
        EventoReconocimiento evento = eventoPendiente();
        when(eventoReconocimientoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));

        service.enviar(EVENTO_ID);

        ArgumentCaptor<EventoCertificacionRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(EventoCertificacionRequestDTO.class);
        verify(certificacionEventosClient).enviar(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(requestCaptor.getValue().getEventoGenerado()).isEqualTo("primer_itinerario_generado");

        ArgumentCaptor<EventoReconocimiento> eventoCaptor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).saveAndFlush(eventoCaptor.capture());
        assertThat(eventoCaptor.getValue().getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.ENVIADO);
        assertThat(eventoCaptor.getValue().getUltimoError()).isNull();
        assertThat(eventoCaptor.getValue().getFechaUltimoIntento()).isNotNull();
    }

    @Test
    void fallaDeIntegracion_dejaEventoEnColaDeReintento() {
        EventoReconocimiento evento = eventoPendiente();
        when(eventoReconocimientoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        doThrow(new CertificacionNoDisponibleException("Certificacion no responde."))
                .when(certificacionEventosClient).enviar(any());

        service.enviar(EVENTO_ID);

        ArgumentCaptor<EventoReconocimiento> eventoCaptor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).saveAndFlush(eventoCaptor.capture());
        EventoReconocimiento guardado = eventoCaptor.getValue();
        assertThat(guardado.getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.PENDIENTE_REINTENTO);
        assertThat(guardado.getIntentosEnvio()).isEqualTo(1);
        assertThat(guardado.getUltimoError()).isEqualTo("Certificacion no responde.");
    }

    @Test
    void errorInesperado_dejaEventoEnColaDeReintento() {
        EventoReconocimiento evento = eventoPendiente();
        when(eventoReconocimientoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        doThrow(new IllegalStateException("Fallo inesperado"))
                .when(certificacionEventosClient).enviar(any());

        service.enviar(EVENTO_ID);

        ArgumentCaptor<EventoReconocimiento> eventoCaptor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).saveAndFlush(eventoCaptor.capture());
        EventoReconocimiento guardado = eventoCaptor.getValue();
        assertThat(guardado.getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.PENDIENTE_REINTENTO);
        assertThat(guardado.getIntentosEnvio()).isEqualTo(1);
        assertThat(guardado.getUltimoError()).isEqualTo("No se pudo enviar el evento a Certificacion.");
    }

    @Test
    void reintentoProcesaEventosPendientes() {
        EventoReconocimiento evento = eventoPendiente();
        evento.setEstadoEnvio(EstadoEnvioCertificacion.PENDIENTE_REINTENTO);
        when(eventoReconocimientoRepository.findTop50ByEstadoEnvioOrderByFechaEventoAsc(
                EstadoEnvioCertificacion.PENDIENTE_REINTENTO)).thenReturn(List.of(evento));
        when(eventoReconocimientoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));

        service.reenviarPendientes();

        verify(certificacionEventosClient).enviar(any(EventoCertificacionRequestDTO.class));
    }

    private static EventoReconocimiento eventoPendiente() {
        return EventoReconocimiento.builder()
                .id(EVENTO_ID)
                .usuarioId(USUARIO_ID)
                .eventoGenerado("primer_itinerario_generado")
                .fechaEvento(Instant.parse("2026-07-22T18:00:00Z"))
                .estadoEnvio(EstadoEnvioCertificacion.PENDIENTE_ENVIO)
                .build();
    }
}
