package com.piedpiper.carbonhub.empresa.service;

import com.piedpiper.carbonhub.empresa.mappers.EmpresaMapperImpl;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaRequestDTO;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionInicialEmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private ConfiguracionInicialEmpresaService service() {
        return new ConfiguracionInicialEmpresaService(empresaRepository, usuarioRepository, new EmpresaMapperImpl());
    }

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private Usuario admin() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("admin@acme.com")
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .build();
    }

    private ConfiguracionInicialEmpresaRequestDTO request() {
        return new ConfiguracionInicialEmpresaRequestDTO(
                "Acme S.A.", "3-101-123456", SectorIndustrial.MANUFACTURA, "CR", 50, "Empresa de prueba.");
    }

    @Test
    void completarConfiguracionExitoso_creaEmpresaConCorreoDelUsuarioYVinculaAlAdmin() {
        Usuario admin = admin();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(false);
        when(empresaRepository.existsBySlug("acme-s-a")).thenReturn(false);
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        ConfiguracionInicialEmpresaResponseDTO response =
                service().completarConfiguracionEmpresa(USUARIO_ID, request());

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).saveAndFlush(empresaCaptor.capture());
        Empresa empresaGuardada = empresaCaptor.getValue();
        assertThat(empresaGuardada.getCorreoCorporativo()).isEqualTo("admin@acme.com");
        assertThat(empresaGuardada.getSlug()).isEqualTo("acme-s-a");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getEmpresa()).isSameAs(empresaGuardada);
        assertThat(usuarioCaptor.getValue().isConfiguracionCompleta()).isFalse();

        assertThat(response.getEmpresaId()).isEqualTo(empresaGuardada.getId());
        assertThat(response.getSlug()).isEqualTo("acme-s-a");
        assertThat(response.isDocumentosPendientes()).isTrue();
        assertThat(response.isRecienCreada()).isTrue();
    }

    @Test
    void rolDistintoDeAdministradorEmpresa_lanza403YNoPersiste() {
        Usuario usuario = admin();
        usuario.setRol(Rol.USUARIO_INDIVIDUAL);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        ConfiguracionInicialEmpresaService servicio = service();
        ConfiguracionInicialEmpresaRequestDTO request = request();

        assertThatThrownBy(() -> servicio.completarConfiguracionEmpresa(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(empresaRepository, never()).saveAndFlush(any());
    }

    @Test
    void usuarioYaTieneEmpresa_devuelve200ConDatosExistentes() {
        Empresa empresaExistente = Empresa.builder()
                .id(UUID.randomUUID())
                .nombreEmpresa("Acme Existente S.A.")
                .slug("acme-existente-s-a")
                .build();
        Usuario usuario = admin();
        usuario.setEmpresa(empresaExistente);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConfiguracionInicialEmpresaResponseDTO response =
                service().completarConfiguracionEmpresa(USUARIO_ID, request());

        assertThat(response.getEmpresaId()).isEqualTo(empresaExistente.getId());
        assertThat(response.getNombreEmpresa()).isEqualTo("Acme Existente S.A.");
        assertThat(response.getSlug()).isEqualTo("acme-existente-s-a");
        assertThat(response.isDocumentosPendientes()).isTrue();
        assertThat(response.isRecienCreada()).isFalse();

        verify(empresaRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void cedulaJuridicaDuplicada_lanza409YNoPersiste() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin()));
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(true);
        ConfiguracionInicialEmpresaService servicio = service();
        ConfiguracionInicialEmpresaRequestDTO request = request();

        assertThatThrownBy(() -> servicio.completarConfiguracionEmpresa(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(empresaRepository, never()).saveAndFlush(any());
    }

    @Test
    void condicionDeCarreraAlGuardarEmpresa_lanza409YNoVinculaUsuario() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin()));
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(false);
        when(empresaRepository.existsBySlug("acme-s-a")).thenReturn(false);
        when(empresaRepository.saveAndFlush(any(Empresa.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        ConfiguracionInicialEmpresaService servicio = service();
        ConfiguracionInicialEmpresaRequestDTO request = request();

        assertThatThrownBy(() -> servicio.completarConfiguracionEmpresa(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }
}
