package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.mappers.UsuarioAuthMapper;
import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionCorreoRequestDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.models.enums.EstadoInvitacion;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroInvitacionCorreoServiceTest {

    @Mock
    private InvitacionService invitacionService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private UsuarioAuthMapper usuarioAuthMapper;

    @InjectMocks
    private RegistroInvitacionCorreoService service;

    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private Invitacion invitacion() {
        return Invitacion.builder()
                .id(UUID.randomUUID())
                .email("colab@correo.com")
                .empresa(Empresa.builder().id(EMPRESA_ID).nombreEmpresa("Acme S.A.").build())
                .tokenHash("hash")
                .estado(EstadoInvitacion.ENVIADA)
                .fechaEmision(Instant.now())
                .fechaExpiracion(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
    }

    private RegistroInvitacionCorreoRequestDTO request() {
        return new RegistroInvitacionCorreoRequestDTO(
                "token-invitacion", "Ana", "Torres", "clave1234", "clave1234", true);
    }

    @Test
    void registroExitosoCreaUsuarioGeneralActivoVinculadoYMarcaLaInvitacionAceptada() {
        Invitacion invitacion = invitacion();
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion);
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");
        // RedirectResolver.paraUsuario resuelve a este mismo valor para un USUARIO_GENERAL con
        // configuracionCompleta=false (el default al construir el usuario), asi que el stub sigue aplicando.
        when(usuarioAuthMapper.toAuthResponse(any(Usuario.class), eq("jwt-app"), eq("/perfil/configuracion-inicial")))
                .thenReturn(new AuthResponseDTO(
                        "jwt-app", "USUARIO_GENERAL", "ACTIVO", "/perfil/configuracion-inicial"));

        AuthResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getEmail()).isEqualTo("colab@correo.com");
        assertThat(creado.getNombre()).isEqualTo("Ana");
        assertThat(creado.getApellidos()).isEqualTo("Torres");
        assertThat(creado.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(creado.getPasswordHash()).isNotEqualTo("clave1234");
        assertThat(creado.getRol()).isEqualTo(Rol.USUARIO_GENERAL);
        assertThat(creado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(creado.getMetodoAuth()).isEqualTo(MetodoAuth.CORREO);
        assertThat(creado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);

        verify(invitacionService).marcarAceptada(invitacion);

        ArgumentCaptor<Usuario> mapperCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioAuthMapper).toAuthResponse(mapperCaptor.capture(), eq("jwt-app"),
                eq("/perfil/configuracion-inicial"));
        assertThat(mapperCaptor.getValue().getEmail()).isEqualTo("colab@correo.com");

        assertThat(response.getToken()).isEqualTo("jwt-app");
        assertThat(response.getRol()).isEqualTo("USUARIO_GENERAL");
        assertThat(response.getEstado()).isEqualTo("ACTIVO");
        assertThat(response.getRedirect()).isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void nombreYApellidosConEspaciosAlrededorQuedanRecortados() {
        RegistroInvitacionCorreoRequestDTO requestConEspacios = new RegistroInvitacionCorreoRequestDTO(
                "token-invitacion", "  Ana  ", "  Torres  ", "clave1234", "clave1234", true);
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");
        when(usuarioAuthMapper.toAuthResponse(any(Usuario.class), eq("jwt-app"), eq("/perfil/configuracion-inicial")))
                .thenReturn(new AuthResponseDTO(
                        "jwt-app", "USUARIO_GENERAL", "ACTIVO", "/perfil/configuracion-inicial"));

        service.registrar(requestConEspacios);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana");
        assertThat(captor.getValue().getApellidos()).isEqualTo("Torres");
    }

    @Test
    void correoDuplicadoPreexistenteLanza409SinTocarLaInvitacionNiElUsuario() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(invitacionService, never()).marcarAceptada(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void condicionDeCarreraEnElGuardadoLanza409SinMarcarLaInvitacionAceptada() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("email duplicado"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(invitacionService, never()).marcarAceptada(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void fallaInesperadaEnElGuardadoLanza500SinMarcarLaInvitacionAceptada() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new RuntimeException("fallo inesperado de base de datos"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(invitacionService, never()).marcarAceptada(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void fallaInesperadaAlMarcarLaInvitacionAceptadaLanza500EnVezDePropagarse() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("fallo inesperado al marcar la invitacion"))
                .when(invitacionService).marcarAceptada(any(Invitacion.class));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void fallaInesperadaAlGenerarElTokenLanza500EnVezDePropagarse() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class)))
                .thenThrow(new RuntimeException("fallo inesperado al generar el token"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(invitacionService).marcarAceptada(any(Invitacion.class));
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void tokenDeInvitacionInvalidoPropagaLaExcepcionSinTocarElUsuario() {
        when(invitacionService.validarParaAceptar("token-invitacion"))
                .thenThrow(ApiException.invitacionInvalida());

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void invitacionRevocadaPropagaLaExcepcionSinTocarElUsuario() {
        when(invitacionService.validarParaAceptar("token-invitacion"))
                .thenThrow(ApiException.invitacionNoDisponible());

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }

    @Test
    void invitacionExpiradaPropagaLaExcepcionSinTocarElUsuario() {
        when(invitacionService.validarParaAceptar("token-invitacion"))
                .thenThrow(ApiException.invitacionExpirada());

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);

        verify(usuarioRepository, never()).existsByEmailIgnoreCase(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(usuarioAuthMapper, never()).toAuthResponse(any(), any(), any());
    }
}
