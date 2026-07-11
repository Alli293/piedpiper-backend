package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasResponseDTO;
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
class PreferenciasServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PreferenciasService service;

    private Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("maria@carbonhub.cr")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    // --- Defaults ---

    @Test
    void cuentaNuevaTieneDefaultsEspanolCrcMetrico() {
        Usuario nuevo = usuario();

        assertThat(nuevo.getIdioma()).isEqualTo("ESPANOL");
        assertThat(nuevo.getMoneda()).isEqualTo("CRC");
        assertThat(nuevo.getUnidades()).isEqualTo("METRICO");
    }

    // --- Actualización ---

    @Test
    void actualizarPersisteSoloValoresSoportados() {
        Usuario existente = usuario();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));

        PreferenciasResponseDTO respuesta = service.actualizar(USUARIO_ID,
                new PreferenciasRequestDTO("INGLES", "USD", "METRICO"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getIdioma()).isEqualTo("INGLES");
        assertThat(captor.getValue().getMoneda()).isEqualTo("USD");
        assertThat(captor.getValue().getUnidades()).isEqualTo("METRICO");

        assertThat(respuesta.getIdioma()).isEqualTo("INGLES");
        assertThat(respuesta.getMoneda()).isEqualTo("USD");
        assertThat(respuesta.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void idiomaFueraDeCatalogoRechazaCon422YNoPersiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizar(USUARIO_ID,
                new PreferenciasRequestDTO("FRANCES", "CRC", "METRICO")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void monedaFueraDeCatalogoRechazaCon422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizar(USUARIO_ID,
                new PreferenciasRequestDTO("ESPANOL", "EUR", "METRICO")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void unidadesFueraDeCatalogoRechazaCon422() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));

        assertThatThrownBy(() -> service.actualizar(USUARIO_ID,
                new PreferenciasRequestDTO("ESPANOL", "CRC", "IMPERIAL")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void usuarioInexistenteRechazaCon404() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(USUARIO_ID,
                new PreferenciasRequestDTO("ESPANOL", "CRC", "METRICO")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // --- Lectura ---

    @Test
    void obtenerDevuelveValoresAlmacenados() {
        Usuario existente = usuario();
        existente.setIdioma("INGLES");
        existente.setMoneda("USD");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));

        PreferenciasResponseDTO respuesta = service.obtener(USUARIO_ID);

        assertThat(respuesta.getIdioma()).isEqualTo("INGLES");
        assertThat(respuesta.getMoneda()).isEqualTo("USD");
        assertThat(respuesta.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void valorAlmacenadoNoSoportadoAplicaDefaultSinError() {
        Usuario existente = usuario();
        existente.setIdioma("KLINGON");
        existente.setMoneda("EUR");
        existente.setUnidades("IMPERIAL");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));

        PreferenciasResponseDTO respuesta = service.obtener(USUARIO_ID);

        assertThat(respuesta.getIdioma()).isEqualTo("ESPANOL");
        assertThat(respuesta.getMoneda()).isEqualTo("CRC");
        assertThat(respuesta.getUnidades()).isEqualTo("METRICO");
    }

    @Test
    void valorAlmacenadoNuloAplicaDefaultSinError() {
        Usuario existente = usuario();
        existente.setIdioma(null);
        existente.setMoneda(null);
        existente.setUnidades(null);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));

        PreferenciasResponseDTO respuesta = service.obtener(USUARIO_ID);

        assertThat(respuesta.getIdioma()).isEqualTo("ESPANOL");
        assertThat(respuesta.getMoneda()).isEqualTo("CRC");
        assertThat(respuesta.getUnidades()).isEqualTo("METRICO");
    }
}
