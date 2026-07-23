package com.piedpiper.carbonhub.reconocimiento.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.reconocimiento.mappers.EventoReconocimientoMapper;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoReconocimientoResponseDTO;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.RegistrarEventoReconocimientoRequestDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.repository.EventoReconocimientoRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoReconocimientoServiceTest {

    @Mock
    private EventoReconocimientoRepository eventoReconocimientoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EventoReconocimientoEnvioService eventoReconocimientoEnvioService;
    @Mock
    private EventoReconocimientoMapper eventoReconocimientoMapper;

    @InjectMocks
    private EventoReconocimientoService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Test
    void eventoGenerado_registraTimestampUtcYEnviaACertificacion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(eventoReconocimientoRepository.save(any(EventoReconocimiento.class)))
                .thenAnswer(invocation -> {
                    EventoReconocimiento evento = invocation.getArgument(0);
                    evento.setId(UUID.randomUUID());
                    return evento;
                });
        when(eventoReconocimientoMapper.toDto(any(EventoReconocimiento.class)))
                .thenAnswer(invocation -> response(invocation.getArgument(0)));

        EventoReconocimientoResponseDTO response = service.registrar(
                new RegistrarEventoReconocimientoRequestDTO(USUARIO_ID, "primer_itinerario_generado"),
                USUARIO_ID);

        ArgumentCaptor<EventoReconocimiento> captor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).save(captor.capture());
        EventoReconocimiento guardado = captor.getValue();
        assertThat(guardado.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(guardado.getEventoGenerado()).isEqualTo("primer_itinerario_generado");
        assertThat(guardado.getFechaEvento()).isNotNull();
        assertThat(guardado.getFechaEvento()).isBeforeOrEqualTo(Instant.now());
        assertThat(guardado.getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.PENDIENTE_ENVIO);

        verify(eventoReconocimientoEnvioService).enviar(guardado.getId());
        assertThat(response.getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.PENDIENTE_ENVIO);
    }

    @Test
    void eventoFueraDeCatalogo_registraSinNotificarACertificacion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(EstadoUsuario.ACTIVO)));
        when(eventoReconocimientoRepository.save(any(EventoReconocimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoReconocimientoMapper.toDto(any(EventoReconocimiento.class)))
                .thenAnswer(invocation -> response(invocation.getArgument(0)));

        EventoReconocimientoResponseDTO response = service.registrar(
                new RegistrarEventoReconocimientoRequestDTO(USUARIO_ID, "evento_desconocido"),
                USUARIO_ID);

        ArgumentCaptor<EventoReconocimiento> captor = ArgumentCaptor.forClass(EventoReconocimiento.class);
        verify(eventoReconocimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getEstadoEnvio()).isEqualTo(EstadoEnvioCertificacion.FUERA_CATALOGO);
        assertThat(response.getEventoGenerado()).isEqualTo("evento_desconocido");
        verify(eventoReconocimientoEnvioService, never()).enviar(any());
    }

    @Test
    void usuarioDistintoAlAutenticado_rechazaCon403() {
        UUID otroUsuarioId = UUID.randomUUID();

        assertThatThrownBy(() -> service.registrar(
                new RegistrarEventoReconocimientoRequestDTO(otroUsuarioId, "primer_itinerario_generado"),
                USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(eventoReconocimientoRepository, never()).save(any());
    }

    @Test
    void usuarioInactivo_rechazaCon403YNoRegistraEvento() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(EstadoUsuario.DESHABILITADO)));

        assertThatThrownBy(() -> service.registrar(
                new RegistrarEventoReconocimientoRequestDTO(USUARIO_ID, "primer_itinerario_generado"),
                USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(eventoReconocimientoRepository, never()).save(any());
    }

    private static Usuario usuario(EstadoUsuario estado) {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("usuario@carbonhub.test")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(estado)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private static EventoReconocimientoResponseDTO response(EventoReconocimiento evento) {
        return new EventoReconocimientoResponseDTO(
                evento.getId(),
                evento.getUsuarioId(),
                evento.getEventoGenerado(),
                evento.getFechaEvento(),
                evento.getEstadoEnvio());
    }
}
