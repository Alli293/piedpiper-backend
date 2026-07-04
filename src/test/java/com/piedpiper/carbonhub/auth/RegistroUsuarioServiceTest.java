package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.RegistroUsuarioRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.user.EstadoUsuario;
import com.piedpiper.carbonhub.user.Rol;
import com.piedpiper.carbonhub.user.Usuario;
import com.piedpiper.carbonhub.user.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroUsuarioServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistroUsuarioService service;

    private RegistroUsuarioRequest request() {
        return new RegistroUsuarioRequest("token-google", true);
    }

    @Test
    void registroExitosoCreaUsuarioIndividualYEmiteToken() {
        when(googleTokenVerifier.verificar("token-google"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");

        AuthResponse response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.USUARIO_INDIVIDUAL);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(response.token()).isEqualTo("jwt-app");
        assertThat(response.redirect()).isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void correoNoVerificadoLanza422YNoPersiste() {
        when(googleTokenVerifier.verificar("token-google"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", false, "Ana"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void subDuplicadoLanza409YNoPersiste() {
        when(googleTokenVerifier.verificar("token-google"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }
}
