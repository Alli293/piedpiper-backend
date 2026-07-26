package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioCorreoRequestDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;
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
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroUsuarioCorreoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificacionService emailVerificacionService;

    @InjectMocks
    private RegistroUsuarioCorreoService service;

    private RegistroUsuarioCorreoRequestDTO request() {
        return new RegistroUsuarioCorreoRequestDTO(
                "Ana", "Perez", "ana.perez@example.com", "clave123", "clave123", true);
    }

    @Test
    void registroExitoso_guardaPendienteDeVerificacionConContrasenaHasheadaYEnviaCorreo() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana.perez@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        RegistroPendienteResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getApellidos()).isEqualTo("Perez");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(guardado.getPasswordHash()).isNotEqualTo("clave123");
        assertThat(guardado.getRol()).isEqualTo(Rol.USUARIO_INDIVIDUAL);
        assertThat(guardado.getMetodoAuth()).isEqualTo(MetodoAuth.CORREO);
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(guardado.getTokenVerificacionHash()).isNotNull().isNotBlank();
        assertThat(guardado.getTokenVerificacionExpiracion())
                .isAfter(Instant.now())
                .isCloseTo(Instant.now().plus(Duration.ofHours(24)),
                        new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES));

        ArgumentCaptor<String> tokenPlanoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificacionService)
                .enviarCorreoVerificacion(eq("Ana"), eq("ana.perez@example.com"), tokenPlanoCaptor.capture());
        String tokenPlano = tokenPlanoCaptor.getValue();
        assertThat(tokenPlano).isNotEqualTo(guardado.getTokenVerificacionHash());
        assertThat(TokenVerificacionGenerator.hash(tokenPlano)).isEqualTo(guardado.getTokenVerificacionHash());
        assertThat(response.getEmail()).isEqualTo("ana.perez@example.com");
        assertThat(response.getMensaje())
                .isEqualTo("Te enviamos un correo de verificación a tu bandeja de entrada.");
    }

    @Test
    void emailDuplicadoPreexistente_lanza409YNoPersiste() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana.perez@example.com")).thenReturn(true);

        RegistroUsuarioCorreoRequestDTO solicitud = request();
        assertThatThrownBy(() -> service.registrar(solicitud))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(emailVerificacionService, never()).enviarCorreoVerificacion(any(), any(), any());
    }
}
