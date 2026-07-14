package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioResponseDTO;
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
import org.springframework.dao.DataAccessResourceFailureException;
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
class PreferenciasUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PreferenciasUsuarioService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("usuario@correo.com")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    @Test
    void cuentaNuevaTieneDefaults_espanolCrcMetrico() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        PreferenciasUsuarioResponseDTO response = service.obtenerPreferencias(USUARIO_ID);

        assertThat(response.getIdioma()).isEqualTo("ESPANOL");
        assertThat(response.getMoneda()).isEqualTo("CRC");
        assertThat(response.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void actualizarConValoresSoportados_persisteYDevuelveLasPreferencias() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        PreferenciasUsuarioResponseDTO response = service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("INGLES", "USD", "METRICO"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getIdioma()).isEqualTo("INGLES");
        assertThat(guardado.getMoneda()).isEqualTo("USD");
        assertThat(guardado.getUnidades()).isEqualTo("METRICO");

        assertThat(response.getIdioma()).isEqualTo("INGLES");
        assertThat(response.getMoneda()).isEqualTo("USD");
        assertThat(response.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void idiomaFueraDeCatalogo_rechazaCon422YNoPersiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("FRANCES", "CRC", "METRICO")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void monedaFueraDeCatalogo_rechazaCon422YNoPersiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("ESPANOL", "EUR", "METRICO")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void unidadesFueraDeCatalogo_rechazaCon422YNoPersiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("ESPANOL", "CRC", "IMPERIAL")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void valorAlmacenadoYaNoSoportado_aplicaElDefaultSinError() {
        Usuario usuario = usuario();
        usuario.setIdioma("KLINGON");
        usuario.setMoneda("EUR");
        usuario.setUnidades("IMPERIAL");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        PreferenciasUsuarioResponseDTO response = service.obtenerPreferencias(USUARIO_ID);

        assertThat(response.getIdioma()).isEqualTo("ESPANOL");
        assertThat(response.getMoneda()).isEqualTo("CRC");
        assertThat(response.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void valorAlmacenadoNulo_aplicaElDefaultSinError() {
        Usuario usuario = usuario();
        usuario.setIdioma(null);
        usuario.setMoneda(null);
        usuario.setUnidades(null);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        PreferenciasUsuarioResponseDTO response = service.obtenerPreferencias(USUARIO_ID);

        assertThat(response.getIdioma()).isEqualTo("ESPANOL");
        assertThat(response.getMoneda()).isEqualTo("CRC");
        assertThat(response.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void fallaDePersistencia_lanza500ConMensajeDeReintento() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        assertThatThrownBy(() -> service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("INGLES", "USD", "METRICO")))
                .isInstanceOf(ApiException.class)
                .hasMessage("No se pudieron guardar tus preferencias. Intenta nuevamente.")
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void usuarioNoEncontrado_rechazaCon403() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPreferencias(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void variosValoresInvalidos_reportaTodosLosErroresEnUnSolo422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizarPreferencias(
                USUARIO_ID, new PreferenciasUsuarioRequestDTO("FRANCES", "EUR", "IMPERIAL")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("idioma")
                .hasMessageContaining("moneda")
                .hasMessageContaining("unidades")
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }
}
