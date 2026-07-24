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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class EventoReconocimientoIntentoEnvioServiceTest {

    @Mock
    private EventoReconocimientoRepository eventoReconocimientoRepository;
    @Mock
    private CertificacionEventosClient certificacionEventosClient;

    @InjectMocks
    private EventoReconocimientoIntentoEnvioService service;

    private static final UUID EVENTO_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Test
    void envioCorrecto_marcaEventoComoEnviado() {
        EventoReconocimiento evento = eventoPendiente(0);
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
        EventoReconocimiento evento = eventoPendiente(0);
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
        EventoReconocimiento evento = eventoPendiente(0);
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
    void tercerFalloMarcaReintentosAgotadosYRegistraError(CapturedOutput output) {
        EventoReconocimiento evento = eventoPendiente(2);
        when(eventoReconocimientoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        doThrow(new CertificacionNoDisponibleException("Certificacion no responde."))
                .when(certificacionEventosClient).enviar(any());

        service.enviar(EVENTO_ID);

        ArgumentCaptor<EventoReconocimiento> eventoCaptor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).saveAndFlush(eventoCaptor.capture());
        EventoReconocimiento guardado = eventoCaptor.getValue();
        assertThat(guardado.getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.REINTENTOS_AGOTADOS);
        assertThat(guardado.getIntentosEnvio()).isEqualTo(3);
        assertThat(guardado.getUltimoError()).isEqualTo("Certificacion no responde.");
        assertThat(output.getAll()).contains("agoto 3 reintentos de envio a Certificacion");
    }

    private static EventoReconocimiento eventoPendiente(int intentosEnvio) {
        return EventoReconocimiento.builder()
                .id(EVENTO_ID)
                .usuarioId(USUARIO_ID)
                .eventoGenerado("primer_itinerario_generado")
                .fechaEvento(Instant.parse("2026-07-22T18:00:00Z"))
                .estadoEnvio(EstadoEnvioCertificacion.PENDIENTE_ENVIO)
                .intentosEnvio(intentosEnvio)
                .build();
    }
}
