package com.piedpiper.carbonhub.auth;

import com.piedpiper.carbonhub.auth.dto.AuthResponse;
import com.piedpiper.carbonhub.auth.dto.RegistroEmpresaRequest;
import com.piedpiper.carbonhub.auth.google.GoogleClaims;
import com.piedpiper.carbonhub.auth.google.GoogleTokenVerifier;
import com.piedpiper.carbonhub.auth.jwt.JwtService;
import com.piedpiper.carbonhub.common.ApiException;
import com.piedpiper.carbonhub.empresa.Empresa;
import com.piedpiper.carbonhub.empresa.EmpresaRepository;
import com.piedpiper.carbonhub.empresa.SectorIndustrial;
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
class RegistroEmpresaServiceTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegistroEmpresaService service;

    private RegistroEmpresaRequest request() {
        return new RegistroEmpresaRequest("token", "Acme", SectorIndustrial.SERVICIOS, "CR",
                12, "info@acme.com", true);
    }

    @Test
    void registroExitosoCreaEmpresaYAdministrador() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "rep@gmail.com", true, "Rep"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("rep@gmail.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativoIgnoreCase("info@acme.com")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generar(any(Usuario.class))).thenReturn("jwt-app");

        AuthResponse response = service.registrar(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.ADMINISTRADOR_EMPRESA);
        assertThat(captor.getValue().getEmpresa()).isNotNull();
        assertThat(response.redirect()).isEqualTo("/empresa/configuracion-inicial");
    }

    @Test
    void correoCorporativoDuplicadoLanza409YNoPersiste() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "rep@gmail.com", true, "Rep"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(false);
        when(usuarioRepository.existsByEmail("rep@gmail.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativoIgnoreCase("info@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(empresaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void representanteDuplicadoLanza409YNoPersiste() {
        when(googleTokenVerifier.verificar("token"))
                .thenReturn(new GoogleClaims("sub-1", "rep@gmail.com", true, "Rep"));
        when(usuarioRepository.existsByGoogleSub("sub-1")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(empresaRepository, never()).save(any());
    }
}
