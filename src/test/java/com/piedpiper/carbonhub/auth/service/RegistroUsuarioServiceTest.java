package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroUsuarioRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
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

    private RegistroUsuarioRequestDTO request() {
        return new RegistroUsuarioRequestDTO("token-google", true);
    }

    @Test
    void registroExitosoCreaUsuarioIndividualYEmiteToken() {
        when(googleTokenVerifier.verificar("token-google"))
                .thenReturn(new GoogleClaims("sub-1", "ana@gmail.com", true, "Ana"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("ana@gmail.com")).thenReturn(false);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");

        AuthResponseDTO response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.USUARIO_INDIVIDUAL);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(response.getToken()).isEqualTo("jwt-app");
        assertThat(response.getRedirect()).isEqualTo("/perfil/configuracion-inicial");
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
