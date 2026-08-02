package com.piedpiper.carbonhub.limite.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaAutenticadaServiceTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EmpresaAutenticadaService service;

    private Authentication authentication(String usuarioId) {
        return new TestingAuthenticationToken(usuarioId, "password", "ROLE_ADMINISTRADOR_EMPRESA");
    }

    @Test
    void usuarioConEmpresaDevuelveElId() {
        Usuario usuario = Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        UUID empresaId = service.obtenerEmpresaId(authentication(USUARIO_ID.toString()));

        assertThat(empresaId).isEqualTo(EMPRESA_ID);
    }

    @Test
    void usuarioSinEmpresaLanza422() {
        Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        Authentication auth = authentication(USUARIO_ID.toString());
        assertThatThrownBy(() -> service.obtenerEmpresaId(auth))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioNoEncontradoLanza500() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        Authentication auth = authentication(USUARIO_ID.toString());
        assertThatThrownBy(() -> service.obtenerEmpresaId(auth))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void nombreDeUsuarioInvalidoLanza403() {
        Authentication auth = authentication("no-es-un-uuid");
        assertThatThrownBy(() -> service.obtenerEmpresaId(auth))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
