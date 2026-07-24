package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoReconocimientoEnvioServiceTest {

    @Mock
    private EventoReconocimientoRepository eventoReconocimientoRepository;
    @Mock
    private EventoReconocimientoIntentoEnvioService eventoReconocimientoIntentoEnvioService;

    @InjectMocks
    private EventoReconocimientoEnvioService service;

    private static final UUID EVENTO_ID = UUID.randomUUID();

    @Test
    void envioAsyncDelegaEnElServicioTransaccional() {
        service.enviarAsync(EVENTO_ID);

        verify(eventoReconocimientoIntentoEnvioService).enviar(EVENTO_ID);
    }

    @Test
    void reintentoProcesaEventosPendientesDesdeBeanTransaccionalSeparado() {
        EventoReconocimiento evento = EventoReconocimiento.builder()
                .id(EVENTO_ID)
                .usuarioId(UUID.randomUUID())
                .eventoGenerado("primer_itinerario_generado")
                .fechaEvento(Instant.parse("2026-07-22T18:00:00Z"))
                .estadoEnvio(EstadoEnvioCertificacion.PENDIENTE_REINTENTO)
                .build();
        when(eventoReconocimientoRepository.findTop50ByEstadoEnvioOrderByFechaEventoAsc(
                EstadoEnvioCertificacion.PENDIENTE_REINTENTO)).thenReturn(List.of(evento));

        service.reenviarPendientes();

        verify(eventoReconocimientoIntentoEnvioService).enviar(EVENTO_ID);
    }
}
