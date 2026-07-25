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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoReconocimientoPersistenciaServiceTest {

    @Mock
    private EventoReconocimientoRepository eventoReconocimientoRepository;

    @InjectMocks
    private EventoReconocimientoPersistenciaService service;

    @Test
    void guardarNuevoFuerzaFlushParaDetectarDuplicadosEnLaTransaccion() {
        EventoReconocimiento evento = EventoReconocimiento.builder()
                .usuarioId(UUID.randomUUID())
                .eventoGenerado("primer_itinerario_generado")
                .fechaEvento(Instant.parse("2026-07-24T18:00:00Z"))
                .estadoEnvio(EstadoEnvioCertificacion.PENDIENTE_ENVIO)
                .build();
        when(eventoReconocimientoRepository.saveAndFlush(evento)).thenReturn(evento);

        EventoReconocimiento guardado = service.guardarNuevo(evento);

        assertThat(guardado).isSameAs(evento);
        verify(eventoReconocimientoRepository).saveAndFlush(evento);
    }
}
