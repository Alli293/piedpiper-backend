package com.piedpiper.carbonhub.emision.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionEmpresaServiceTest {
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EmisionEmpresaService service;

    @Test
    void usuarioConEmpresaDevuelveEmpresaId() {
        Usuario usuario = Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().id(EMPRESA_ID).build())
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        assertThat(service.empresaId(USUARIO_ID)).isEqualTo(EMPRESA_ID);
    }

    @Test
    void usuarioSinEmpresaDevuelve422() {
        Usuario usuario = Usuario.builder().id(USUARIO_ID).build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.empresaId(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioConEmpresaSinIdDevuelve422() {
        Usuario usuario = Usuario.builder()
                .id(USUARIO_ID)
                .empresa(Empresa.builder().build())
                .build();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.empresaId(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void usuarioNoEncontradoDevuelve500() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.empresaId(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
