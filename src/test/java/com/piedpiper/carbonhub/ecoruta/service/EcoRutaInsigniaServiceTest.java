package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.config.CatalogoInsigniasEcoRuta;
import com.piedpiper.carbonhub.ecoruta.mappers.InsigniaUsuarioMapperImpl;
import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import com.piedpiper.carbonhub.ecoruta.repository.InsigniaUsuarioRepository;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcoRutaInsigniaServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final Instant FECHA_EVENTO = Instant.parse("2026-07-15T20:32:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InsigniaUsuarioRepository insigniaUsuarioRepository;

    private EcoRutaInsigniaService service;

    @BeforeEach
    void setUp() {
        service = new EcoRutaInsigniaService(usuarioRepository, insigniaUsuarioRepository,
                new CatalogoInsigniasEcoRuta(), new InsigniaUsuarioMapperImpl());
    }

    @Test
    void otorgaInsigniaCuandoEventoCoincideYUsuarioNoLaPosee() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioActivo()));
        when(insigniaUsuarioRepository.existsByUsuarioIdAndIdInsignia(USUARIO_ID, 2L))
                .thenReturn(false);

        service.evaluarYOtorgar(evento(
                CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE));

        ArgumentCaptor<InsigniaUsuario> captor = ArgumentCaptor.forClass(InsigniaUsuario.class);
        verify(insigniaUsuarioRepository).saveAndFlush(captor.capture());
        InsigniaUsuario guardada = captor.getValue();
        assertThat(guardada.getUsuario().getId()).isEqualTo(USUARIO_ID);
        assertThat(guardada.getIdInsignia()).isEqualTo(2L);
        assertThat(guardada.getEventoDesbloqueo())
                .isEqualTo(CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE);
        assertThat(guardada.getFechaObtencion()).isEqualTo(FECHA_EVENTO);
    }

    @Test
    void omiteCuandoLaInsigniaYaExiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioActivo()));
        when(insigniaUsuarioRepository.existsByUsuarioIdAndIdInsignia(USUARIO_ID, 2L))
                .thenReturn(true);

        service.evaluarYOtorgar(evento(
                CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE));

        verify(insigniaUsuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void omiteCuandoElCodigoDeEventoEsDesconocido() {
        service.evaluarYOtorgar(evento("evento_desconocido"));

        verify(usuarioRepository, never()).findById(any());
        verify(insigniaUsuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void omiteCuandoUsuarioNoEsIndividualActivo() {
        Usuario usuario = usuarioActivo();
        usuario.setEstado(EstadoUsuario.DESHABILITADO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        service.evaluarYOtorgar(evento(
                CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE));

        verify(insigniaUsuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void falloDeInsercionNoPropagaExcepcionAlLlamador() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioActivo()));
        when(insigniaUsuarioRepository.existsByUsuarioIdAndIdInsignia(USUARIO_ID, 2L))
                .thenReturn(false);
        when(insigniaUsuarioRepository.saveAndFlush(any(InsigniaUsuario.class)))
                .thenThrow(new RuntimeException("base no disponible"));

        assertThatNoException().isThrownBy(() ->
                service.evaluarYOtorgar(
                        evento(CatalogoInsigniasEcoRuta.EVENTO_PRIMER_ITINERARIO_SOSTENIBLE)));
    }

    private EventoCertificacionRequestDTO evento(String eventoGenerado) {
        return new EventoCertificacionRequestDTO(USUARIO_ID, eventoGenerado, FECHA_EVENTO);
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }
}
