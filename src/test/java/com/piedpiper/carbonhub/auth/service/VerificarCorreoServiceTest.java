package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificarCorreoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private VerificarCorreoService service;

    private Usuario usuarioPendiente(String token, Instant expiracion) {
        return Usuario.builder()
                .email("ana.perez@example.com")
                .nombre("Ana")
                .apellidos("Perez")
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .tokenVerificacion(token)
                .tokenVerificacionExpiracion(expiracion)
                .build();
    }

    @Test
    void tokenValido_activaLaCuentaYLimpiaElToken() {
        Usuario usuario = usuarioPendiente("token-valido", Instant.now().plus(Duration.ofHours(1)));
        when(usuarioRepository.findByTokenVerificacion("token-valido")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.verificar("token-valido");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(guardado.getTokenVerificacion()).isNull();
        assertThat(guardado.getTokenVerificacionExpiracion()).isNull();
        assertThat(response.getMensaje()).isEqualTo("¡Correo verificado! Ya puedes iniciar sesión.");
    }

    @Test
    void tokenInexistente_lanza404YNoPersiste() {
        when(usuarioRepository.findByTokenVerificacion("token-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificar("token-inexistente"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void tokenExpirado_lanza410YNoActivaLaCuenta() {
        Usuario usuario = usuarioPendiente("token-expirado", Instant.now().minus(Duration.ofMinutes(1)));
        when(usuarioRepository.findByTokenVerificacion("token-expirado")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.verificar("token-expirado"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(usuarioRepository, never()).saveAndFlush(any());
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
    }

    @Test
    void usuarioYaActivo_respondeExitoYNoRelanzaError() {
        Usuario usuario = usuarioPendiente("token-ya-usado", Instant.now().plus(Duration.ofHours(1)));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByTokenVerificacion("token-ya-usado")).thenReturn(Optional.of(usuario));

        MensajeResponseDTO response = service.verificar("token-ya-usado");

        assertThat(response.getMensaje()).isEqualTo("¡Correo verificado! Ya puedes iniciar sesión.");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }
}
