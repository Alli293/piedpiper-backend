package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
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
class RegistroEmpresaCorreoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificacionService emailVerificacionService;

    @InjectMocks
    private RegistroEmpresaCorreoService service;

    private RegistroEmpresaCorreoRequestDTO request() {
        return new RegistroEmpresaCorreoRequestDTO(
                "Ana", "Perez", "admin@acme.com", "clave123", "clave123", true);
    }

    @Test
    void registroExitoso_guardaPendienteDeVerificacionConContrasenaHasheadaYEnviaCorreo() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        RegistroPendienteResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario admin = captor.getValue();
        assertThat(admin.getNombre()).isEqualTo("Ana");
        assertThat(admin.getApellidos()).isEqualTo("Perez");
        assertThat(admin.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(admin.getPasswordHash()).isNotEqualTo("clave123");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR_EMPRESA);
        assertThat(admin.getMetodoAuth()).isEqualTo(MetodoAuth.CORREO);
        assertThat(admin.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(admin.isConfiguracionCompleta()).isFalse();
        assertThat(admin.getTokenVerificacionHash()).isNotNull().isNotBlank();
        assertThat(admin.getTokenVerificacionExpiracion())
                .isAfter(Instant.now())
                .isCloseTo(Instant.now().plus(Duration.ofHours(24)),
                        new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES));

        ArgumentCaptor<String> tokenPlanoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificacionService)
                .enviarCorreoVerificacion(eq("Ana"), eq("admin@acme.com"), tokenPlanoCaptor.capture());
        String tokenPlano = tokenPlanoCaptor.getValue();
        assertThat(tokenPlano).isNotEqualTo(admin.getTokenVerificacionHash());
        assertThat(TokenVerificacionGenerator.hash(tokenPlano)).isEqualTo(admin.getTokenVerificacionHash());
        assertThat(response.getEmail()).isEqualTo("admin@acme.com");
        assertThat(response.getMensaje())
                .isEqualTo("Te enviamos un correo de verificación a tu bandeja de entrada.");
    }

    @Test
    void emailAdminDuplicado_lanza409YNoPersiste() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(emailVerificacionService, never()).enviarCorreoVerificacion(any(), any(), any());
    }
}
