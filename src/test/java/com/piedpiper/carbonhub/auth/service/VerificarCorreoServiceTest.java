package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import com.piedpiper.carbonhub.validacion.models.enums.EstadoSolicitud;
import com.piedpiper.carbonhub.validacion.repository.SolicitudValidacionRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificarCorreoServiceTest {

    private static final String TOKEN_VALIDO = "a".repeat(43);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private SolicitudValidacionRepository solicitudValidacionRepository;
    @Mock
    private EnvioCorreoVerificacionService envioCorreoVerificacionService;

    @InjectMocks
    private VerificarCorreoService service;

    private Usuario usuarioPendiente(Rol rol, String tokenPlano, Instant expiracion) {
        return Usuario.builder()
                .email("ana.perez@example.com")
                .nombre("Ana")
                .apellidos("Perez")
                .rol(rol)
                .estado(EstadoUsuario.PENDIENTE_VERIFICACION)
                .tokenVerificacionHash(TokenVerificacionGenerator.hash(tokenPlano))
                .tokenVerificacionExpiracion(expiracion)
                .build();
    }

    // ---------- verificar() ----------

    @Test
    void tokenValidoIndividualActivaLaCuentaYLimpiaElToken() {
        Usuario usuario = usuarioPendiente(Rol.USUARIO_INDIVIDUAL, TOKEN_VALIDO,
                Instant.now().plus(Duration.ofHours(1)));
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.verificar(TOKEN_VALIDO);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(guardado.getTokenVerificacionHash()).isNull();
        assertThat(guardado.getTokenVerificacionExpiracion()).isNull();
        assertThat(response.getMensaje()).isEqualTo("Tu correo fue verificado. Ya puedes iniciar sesión.");
        verify(solicitudValidacionRepository, never()).save(any());
    }

    @Test
    void tokenValidoAuditorQuedaPendienteDeValidacionYCreaLaSolicitud() {
        Usuario usuario = usuarioPendiente(Rol.AUDITOR_CERTIFICADO, TOKEN_VALIDO,
                Instant.now().plus(Duration.ofHours(1)));
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.verificar(TOKEN_VALIDO);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VALIDACION);

        ArgumentCaptor<SolicitudValidacion> solicitudCaptor = ArgumentCaptor.forClass(SolicitudValidacion.class);
        verify(solicitudValidacionRepository).save(solicitudCaptor.capture());
        SolicitudValidacion solicitud = solicitudCaptor.getValue();
        assertThat(solicitud.getAuditor()).isEqualTo(usuario);
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(solicitud.getFechaSolicitud()).isNotNull();

        assertThat(response.getMensaje()).contains("en revisión");
    }

    @Test
    void tokenMalFormadoLanza400SinConsultarLaBase() {
        assertThatThrownBy(() -> service.verificar("token-demasiado-corto"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usuarioRepository, never()).findByTokenVerificacionHashForUpdate(any());
    }

    @Test
    void tokenInexistenteLanza410() {
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void tokenExpiradoLanza410YNoActivaLaCuenta() {
        Usuario usuario = usuarioPendiente(Rol.USUARIO_INDIVIDUAL, TOKEN_VALIDO,
                Instant.now().minus(Duration.ofMinutes(1)));
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.verificar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(usuarioRepository, never()).saveAndFlush(any());
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
    }

    @Test
    void cuentaYaVerificadaLanza409() {
        Usuario usuario = usuarioPendiente(Rol.USUARIO_INDIVIDUAL, TOKEN_VALIDO,
                Instant.now().plus(Duration.ofHours(1)));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.verificar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void cuentaDeshabilitadaConTokenVigenteLanza409ConMensajeDeCuentaNoDisponible() {
        Usuario usuario = usuarioPendiente(Rol.USUARIO_INDIVIDUAL, TOKEN_VALIDO,
                Instant.now().plus(Duration.ofHours(1)));
        usuario.setEstado(EstadoUsuario.DESHABILITADO);
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.verificar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void fallaInesperadaEnLaTransicionLanza500YNoConsumeElToken() {
        Usuario usuario = usuarioPendiente(Rol.USUARIO_INDIVIDUAL, TOKEN_VALIDO,
                Instant.now().plus(Duration.ofHours(1)));
        when(usuarioRepository.findByTokenVerificacionHashForUpdate(TokenVerificacionGenerator.hash(TOKEN_VALIDO)))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new RuntimeException("fallo inesperado de base de datos"));

        assertThatThrownBy(() -> service.verificar(TOKEN_VALIDO))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ---------- reenviar() ----------

    private Usuario usuarioParaReenvio(EstadoUsuario estado) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email("ana.perez@example.com")
                .nombre("Ana")
                .estado(estado)
                .build();
    }

    @Test
    void reenvioValidoGeneraNuevoTokenInvalidaElAnteriorYEnvia() {
        Usuario usuario = usuarioParaReenvio(EstadoUsuario.PENDIENTE_VERIFICACION);
        usuario.setTokenVerificacionHash("hash-viejo");
        when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ana.perez@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.reenviar("ana.perez@example.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getTokenVerificacionHash()).isNotEqualTo("hash-viejo");
        assertThat(guardado.getTokenVerificacionExpiracion()).isAfter(Instant.now());
        assertThat(guardado.getReenvioVerificacionContador()).isEqualTo(1);

        verify(envioCorreoVerificacionService, times(1))
                .enviar(eq("Ana"), eq("ana.perez@example.com"), any());

        assertThat(response.getMensaje())
                .isEqualTo("Si tu cuenta requiere verificación, te enviamos un nuevo enlace.");
    }

    @Test
    void reenvioConCuentaInexistenteDevuelveMensajeUniformeSinEnviarCorreo() {
        when(usuarioRepository.findByEmailIgnoreCaseForUpdate("no-existe@example.com")).thenReturn(Optional.empty());

        MensajeResponseDTO response = service.reenviar("no-existe@example.com");

        assertThat(response.getMensaje())
                .isEqualTo("Si tu cuenta requiere verificación, te enviamos un nuevo enlace.");
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoVerificacionService, never()).enviar(any(), any(), any());
    }

    @Test
    void reenvioConCuentaYaVerificadaDevuelveMensajeUniformeSinEnviarCorreo() {
        Usuario usuario = usuarioParaReenvio(EstadoUsuario.ACTIVO);
        when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ana.perez@example.com")).thenReturn(Optional.of(usuario));

        MensajeResponseDTO response = service.reenviar("ana.perez@example.com");

        assertThat(response.getMensaje())
                .isEqualTo("Si tu cuenta requiere verificación, te enviamos un nuevo enlace.");
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoVerificacionService, never()).enviar(any(), any(), any());
    }

    @Test
    void reenvioExcedeElLimiteDeTresPorHoraLanza429() {
        Usuario usuario = usuarioParaReenvio(EstadoUsuario.PENDIENTE_VERIFICACION);
        usuario.setReenvioVerificacionContador(3);
        usuario.setReenvioVerificacionVentanaInicio(Instant.now().minus(Duration.ofMinutes(10)));
        when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ana.perez@example.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.reenviar("ana.perez@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(envioCorreoVerificacionService, never()).enviar(any(), any(), any());
    }

    @Test
    void reenvioDespuesDeQueExpiraLaVentanaReseteaYPermite() {
        Usuario usuario = usuarioParaReenvio(EstadoUsuario.PENDIENTE_VERIFICACION);
        usuario.setReenvioVerificacionContador(3);
        usuario.setReenvioVerificacionVentanaInicio(Instant.now().minus(Duration.ofHours(2)));
        when(usuarioRepository.findByEmailIgnoreCaseForUpdate("ana.perez@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        MensajeResponseDTO response = service.reenviar("ana.perez@example.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getReenvioVerificacionContador()).isEqualTo(1);
        assertThat(response.getMensaje())
                .isEqualTo("Si tu cuenta requiere verificación, te enviamos un nuevo enlace.");
    }
}
