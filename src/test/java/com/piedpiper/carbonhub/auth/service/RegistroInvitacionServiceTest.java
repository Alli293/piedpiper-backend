package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroInvitacionRequestDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;
import com.piedpiper.carbonhub.invitacion.models.enums.EstadoInvitacion;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroInvitacionServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private InvitacionService invitacionService;

    @InjectMocks
    private RegistroInvitacionService service;

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

    private RegistroInvitacionRequestDTO request() {
        return new RegistroInvitacionRequestDTO("token-invitacion", "id-token", true);
    }

    private GoogleClaims claims(String email, boolean verificado) {
        return new GoogleClaims("sub-1", email, verificado, "Ana Mora", "Ana", "Mora");
    }

    @Test
    void registroExitosoCreaUsuarioGeneralVinculadoYMarcaLaInvitacionAceptada() {
        Invitacion invitacion = invitacion();
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion);
        when(googleTokenVerifier.verificar("id-token")).thenReturn(claims("colab@correo.com", true));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("colab@correo.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");

        AuthResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getRol()).isEqualTo(Rol.USUARIO_GENERAL);
        assertThat(creado.getEmpresa().getId()).isEqualTo(EMPRESA_ID);
        assertThat(creado.isConfiguracionCompleta()).isFalse();
        verify(invitacionService).marcarAceptada(invitacion);
        assertThat(response.getRedirect()).isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void correoDeGoogleDistintoAlInvitadoLanza403SinTocarLaInvitacion() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(googleTokenVerifier.verificar("id-token")).thenReturn(claims("otra@correo.com", true));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(invitacionService, never()).marcarAceptada(any());
    }

    @Test
    void correoCoincidenteConMayusculasEsAceptado() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(googleTokenVerifier.verificar("id-token")).thenReturn(claims("Colab@Correo.com", true));
        when(usuarioRepository.existsByGoogleSub(any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");

        AuthResponseDTO response = service.registrar(request());

        assertThat(response.getToken()).isEqualTo("jwt-app");
    }

    @Test
    void subDuplicadoLanza409SinTocarLaInvitacion() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(googleTokenVerifier.verificar("id-token")).thenReturn(claims("colab@correo.com", true));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(invitacionService, never()).marcarAceptada(any());
    }

    @Test
    void correoNoVerificadoLanza422SinTocarLaInvitacion() {
        when(invitacionService.validarParaAceptar("token-invitacion")).thenReturn(invitacion());
        when(googleTokenVerifier.verificar("id-token")).thenReturn(claims("colab@correo.com", false));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(invitacionService, never()).marcarAceptada(any());
    }
}
