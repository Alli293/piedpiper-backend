package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroAuditorCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.data.TemporalUnitWithinOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroAuditorCorreoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificacionService emailVerificacionService;

    @InjectMocks
    private RegistroAuditorCorreoService service;

    private RegistroAuditorCorreoRequestDTO request() {
        return new RegistroAuditorCorreoRequestDTO(
                "Carlos", "Lopez", "carlos.lopez@example.com", "clave123", true);
    }

    @Test
    void registroExitoso_guardaAuditorPendienteDeVerificacionConRolCorrecto() {
        when(usuarioRepository.existsByEmailIgnoreCase("carlos.lopez@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        RegistroPendienteResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Carlos");
        assertThat(guardado.getApellidos()).isEqualTo("Lopez");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(guardado.getRol()).isEqualTo(Rol.AUDITOR_CERTIFICADO);
        assertThat(guardado.getMetodoAuth()).isEqualTo(MetodoAuth.CORREO);
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(guardado.isConfiguracionCompleta()).isFalse();
        assertThat(guardado.getTokenVerificacionHash()).isNotNull().isNotBlank();
        assertThat(guardado.getTokenVerificacionExpiracion())
                .isAfter(Instant.now())
                .isCloseTo(Instant.now().plus(Duration.ofHours(24)),
                        new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES));

        ArgumentCaptor<String> tokenPlanoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificacionService)
                .enviarCorreoVerificacion(eq("Carlos"), eq("carlos.lopez@example.com"), tokenPlanoCaptor.capture());
        String tokenPlano = tokenPlanoCaptor.getValue();
        assertThat(TokenVerificacionGenerator.hash(tokenPlano)).isEqualTo(guardado.getTokenVerificacionHash());

        assertThat(response.getEmail()).isEqualTo("carlos.lopez@example.com");
        assertThat(response.getMensaje())
                .isEqualTo("Te enviamos un correo de verificación a tu bandeja de entrada.");
    }

    @Test
    void emailDuplicadoPreexistente_lanza409YNoPersiste() {
        when(usuarioRepository.existsByEmailIgnoreCase("carlos.lopez@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(emailVerificacionService, never()).enviarCorreoVerificacion(any(), any(), any());
    }

    @Test
    void errorInesperadoAlGuardar_lanza500() {
        when(usuarioRepository.existsByEmailIgnoreCase("carlos.lopez@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(emailVerificacionService, never()).enviarCorreoVerificacion(any(), any(), any());
    }
}
