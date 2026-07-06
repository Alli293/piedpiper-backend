package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.auth.models.dtos.LoginRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.GoogleClaims;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginService service;

    private Usuario usuarioCorreo() {
        return Usuario.builder()
                .email("ana@gmail.com")
                .rol(Rol.USUARIO_INDIVIDUAL)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .passwordHash("hash")
                .intentosFallidos(0)
                .build();
    }

    @Test
    void loginCorreoExitosoEmiteTokenYReseteaIntentos() {
        Usuario usuario = usuarioCorreo();
        usuario.setIntentosFallidos(3);
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreta", "hash")).thenReturn(true);
        when(jwtService.generar(usuario)).thenReturn("jwt-app");

        AuthResponseDTO response = service.login(
                new LoginRequestDTO(MetodoAuth.CORREO, null, "ana@gmail.com", "secreta"));

        assertThat(response.getToken()).isEqualTo("jwt-app");
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void credencialesIncorrectasLanza401Uniforme() {
        Usuario usuario = usuarioCorreo();
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequestDTO(MetodoAuth.CORREO, null, "ana@gmail.com", "mala")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void quintoIntentoFallidoBloqueaCuenta() {
        Usuario usuario = usuarioCorreo();
        usuario.setIntentosFallidos(4);
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginRequestDTO(MetodoAuth.CORREO, null, "ana@gmail.com", "mala")))
                .isInstanceOf(ApiException.class);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getIntentosFallidos()).isEqualTo(5);
        assertThat(captor.getValue().getBloqueadoHasta()).isAfter(Instant.now());
    }

    @Test
    void cuentaBloqueadaLanza429() {
        Usuario usuario = usuarioCorreo();
        usuario.setIntentosFallidos(5);
        usuario.setBloqueadoHasta(Instant.now().plusSeconds(600));
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login(
                new LoginRequestDTO(MetodoAuth.CORREO, null, "ana@gmail.com", "secreta")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void googleSinCuentaLanza404() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-x", "nuevo@gmail.com", true, "Nuevo"));
        when(usuarioRepository.findByGoogleSub("sub-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new LoginRequestDTO(MetodoAuth.GOOGLE, "token", null, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cuentaDeshabilitadaLanza403() {
        Usuario usuario = usuarioCorreo();
        usuario.setEstado(EstadoUsuario.DESHABILITADO);
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("secreta", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginRequestDTO(MetodoAuth.CORREO, null, "ana@gmail.com", "secreta")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
