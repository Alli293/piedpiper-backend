package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.ValidarTokenResetResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestablecerContrasenaServiceTest {

    private static final String TOKEN_VALIDO = "a".repeat(43);
    private static final String MENSAJE_UNIFORME =
            "Si existe una cuenta con ese correo, te enviamos un enlace para restablecer tu contraseña.";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EnvioCorreoResetContrasenaService envioCorreoResetContrasenaService;

    @InjectMocks
    private RestablecerContrasenaService service;

    private Usuario usuarioCorreo() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("ana.perez@example.com")
                .nombre("Ana")
                .metodoAuth(MetodoAuth.CORREO)
                .build();
    }

    private Usuario usuarioGoogle() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("ana.perez@example.com")
                .nombre("Ana")
                .metodoAuth(MetodoAuth.GOOGLE)
                .build();
    }

    private Usuario usuarioConTokenReset(String tokenPlano, Instant expiracion) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("ana.perez@example.com")
                .nombre("Ana")
                .metodoAuth(MetodoAuth.CORREO)
                .passwordHash("hash-viejo")
                .tokenResetHash(TokenVerificacionGenerator.hash(tokenPlano))
                .tokenResetExpiracion(expiracion)
                .build();
    }

    // ---------- solicitar() ----------

    @Test
    void solicitarConCuentaCorreoGeneraTokenDeUnaHoraYEnviaElResetReal() {
        Usuario usuario = usuarioCorreo();
        when(usuarioRepository.findByEmailIgnoreCase("ana.perez@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.solicitar("ana.perez@example.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getTokenResetHash()).isNotNull();
        assertThat(guardado.getTokenResetExpiracion())
                .isAfter(Instant.now().plus(Duration.ofMinutes(59)))
                .isBefore(Instant.now().plus(Duration.ofMinutes(61)));
        assertThat(guardado.getResetContrasenaContador()).isEqualTo(1);

        verify(envioCorreoResetContrasenaService, never()).enviarUsaGoogle(any(), any());
        verify(envioCorreoResetContrasenaService).enviarReset(eq("Ana"), eq("ana.perez@example.com"), any());
        assertThat(response.getMensaje()).isEqualTo(MENSAJE_UNIFORME);
    }

    @Test
    void solicitarConCuentaGoogleNoGeneraTokenYEnviaElAvisoDeGoogle() {
        Usuario usuario = usuarioGoogle();
        when(usuarioRepository.findByEmailIgnoreCase("ana.perez@example.com")).thenReturn(Optional.of(usuario));

        MensajeResponseDTO response = service.solicitar("ana.perez@example.com");

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoResetContrasenaService).enviarUsaGoogle("Ana", "ana.perez@example.com");
        verify(envioCorreoResetContrasenaService, never()).enviarReset(any(), any(), any());
        assertThat(response.getMensaje()).isEqualTo(MENSAJE_UNIFORME);
    }

    @Test
    void solicitarConCorreoInexistenteDevuelveMensajeUniformeSinEfectos() {
        when(usuarioRepository.findByEmailIgnoreCase("no-existe@example.com")).thenReturn(Optional.empty());

        MensajeResponseDTO response = service.solicitar("no-existe@example.com");

        assertThat(response.getMensaje()).isEqualTo(MENSAJE_UNIFORME);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoResetContrasenaService, never()).enviarReset(any(), any(), any());
        verify(envioCorreoResetContrasenaService, never()).enviarUsaGoogle(any(), any());
    }

    @Test
    void solicitarQueExcedeElLimiteDeTresPorHoraNoEnviaCorreoPeroSigueDevolviendo200Uniforme() {
        Usuario usuario = usuarioCorreo();
        usuario.setResetContrasenaContador(3);
        usuario.setResetContrasenaVentanaInicio(Instant.now().minus(Duration.ofMinutes(10)));
        when(usuarioRepository.findByEmailIgnoreCase("ana.perez@example.com")).thenReturn(Optional.of(usuario));

        MensajeResponseDTO response = service.solicitar("ana.perez@example.com");

        assertThat(response.getMensaje()).isEqualTo(MENSAJE_UNIFORME);
        assertThat(usuario.getResetContrasenaContador()).isEqualTo(3);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoResetContrasenaService, never()).enviarReset(any(), any(), any());
        verify(envioCorreoResetContrasenaService, never()).enviarUsaGoogle(any(), any());
    }

    @Test
    void solicitarDespuesDeQueExpiraLaVentanaReseteaYPermiteEnviar() {
        Usuario usuario = usuarioCorreo();
        usuario.setResetContrasenaContador(3);
        usuario.setResetContrasenaVentanaInicio(Instant.now().minus(Duration.ofHours(2)));
        when(usuarioRepository.findByEmailIgnoreCase("ana.perez@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.solicitar("ana.perez@example.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getResetContrasenaContador()).isEqualTo(1);
        verify(envioCorreoResetContrasenaService).enviarReset(any(), any(), any());
        assertThat(response.getMensaje()).isEqualTo(MENSAJE_UNIFORME);
    }

    // ---------- validarToken() ----------

    @Test
    void validarTokenValidoDevuelveElEmail() {
        Usuario usuario = usuarioConTokenReset(TOKEN_VALIDO, Instant.now().plus(Duration.ofMinutes(30)));
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        ValidarTokenResetResponseDTO response = service.validarToken(TOKEN_VALIDO);

        assertThat(response.getEmail()).isEqualTo("ana.perez@example.com");
    }

    @Test
    void validarTokenMalFormadoLanza400SinConsultarLaBase() {
        assertThatThrownBy(() -> service.validarToken("token-corto"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usuarioRepository, never()).findByTokenResetHash(any());
    }

    @Test
    void validarTokenInexistenteLanza410() {
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarToken(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    void validarTokenExpiradoLanza410() {
        Usuario usuario = usuarioConTokenReset(TOKEN_VALIDO, Instant.now().minus(Duration.ofMinutes(1)));
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.validarToken(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);
    }

    // ---------- restablecer() ----------

    @Test
    void restablecerExitosoActualizaElHashYLimpiaElToken() {
        Usuario usuario = usuarioConTokenReset(TOKEN_VALIDO, Instant.now().plus(Duration.ofMinutes(30)));
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("claveNueva1")).thenReturn("hash-nuevo");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.restablecer(TOKEN_VALIDO, "claveNueva1");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(guardado.getTokenResetHash()).isNull();
        assertThat(guardado.getTokenResetExpiracion()).isNull();
        assertThat(response.getMensaje()).isEqualTo("Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
    }

    @Test
    void restablecerConTokenExpiradoLanza410YNoTocaElPasswordHash() {
        Usuario usuario = usuarioConTokenReset(TOKEN_VALIDO, Instant.now().minus(Duration.ofMinutes(1)));
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.restablecer(TOKEN_VALIDO, "claveNueva1"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(usuarioRepository, never()).saveAndFlush(any());
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-viejo");
    }

    @Test
    void restablecerConFalloAlGuardarLanza500() {
        // El token NO se consume en producción: la limpieza del hash y la actualización
        // del passwordHash viajan en el MISMO saveAndFlush() dentro del MISMO @Transactional
        // (igual que VerificarCorreoService.fallaInesperadaEnLaTransicionLanza500YNoConsumeElToken),
        // así que si el guardado falla, la transacción real hace rollback de ambos campos
        // a nivel de base de datos. Un repositorio mockeado no simula ese rollback (el objeto
        // Usuario en memoria queda mutado igual, aunque el guardado nunca se haya confirmado
        // realmente), así que no se puede verificar esa garantía inspeccionando el objeto en
        // este test — solo confirmamos que el fallo se traduce en 500.
        Usuario usuario = usuarioConTokenReset(TOKEN_VALIDO, Instant.now().plus(Duration.ofMinutes(30)));
        when(usuarioRepository.findByTokenResetHash(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("claveNueva1")).thenReturn("hash-nuevo");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new RuntimeException("fallo inesperado de base de datos"));

        assertThatThrownBy(() -> service.restablecer(TOKEN_VALIDO, "claveNueva1"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
