package com.piedpiper.carbonhub.auth.service;

import com.piedpiper.carbonhub.auth.models.dtos.RegistroEmpresaCorreoRequestDTO;
import com.piedpiper.carbonhub.auth.models.dtos.RegistroPendienteResponseDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.notification.service.EmailVerificacionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroEmpresaCorreoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailVerificacionService emailVerificacionService;

    @InjectMocks
    private RegistroEmpresaCorreoService service;

    private RegistroEmpresaCorreoRequestDTO request() {
        return new RegistroEmpresaCorreoRequestDTO(
                "Acme S.A.", "3-101-123456", SectorIndustrial.MANUFACTURA, "CR", 50,
                "contacto@acme.com", "Ana", "Perez", "admin@acme.com",
                "clave123", "clave123", true);
    }

    @Test
    void registroExitoso_creaEmpresaYAdminVinculadoPendienteDeVerificacion() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativo("contacto@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(false);
        when(empresaRepository.existsBySlug("acme-s-a")).thenReturn(false);
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        RegistroPendienteResponseDTO response = service.registrar(request());

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).saveAndFlush(empresaCaptor.capture());
        Empresa empresaGuardada = empresaCaptor.getValue();
        assertThat(empresaGuardada.getSlug()).isEqualTo("acme-s-a");
        assertThat(empresaGuardada.getEstado()).isEqualTo(EstadoEmpresa.ACTIVO);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
        Usuario admin = usuarioCaptor.getValue();
        assertThat(admin.getNombre()).isEqualTo("Ana");
        assertThat(admin.getApellidos()).isEqualTo("Perez");
        assertThat(admin.getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR_EMPRESA);
        assertThat(admin.getMetodoAuth()).isEqualTo(MetodoAuth.CORREO);
        assertThat(admin.getEstado()).isEqualTo(EstadoUsuario.PENDIENTE_VERIFICACION);
        assertThat(admin.getEmpresa()).isSameAs(empresaGuardada);

        verify(emailVerificacionService).enviarCorreoVerificacion("Ana", "admin@acme.com");
        assertThat(response.getEmail()).isEqualTo("admin@acme.com");
    }

    @Test
    void emailAdminDuplicado_lanza409YNoPersisteNiEmpresaNiUsuario() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(empresaRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void correoCorporativoDuplicado_lanza409YNoPersisteNada() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativo("contacto@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(empresaRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void cedulaJuridicaDuplicada_lanza409YNoPersisteNada() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativo("contacto@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(empresaRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void slugDuplicado_generaSlugConSufijoNumerico() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativo("contacto@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(false);
        when(empresaRepository.existsBySlug("acme-s-a")).thenReturn(true);
        when(empresaRepository.existsBySlug("acme-s-a-2")).thenReturn(false);
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(request());

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("acme-s-a-2");
    }

    @Test
    void fallaAlGuardarUsuarioDespuesDeGuardarEmpresa_lanzaErrorInterno() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCorreoCorporativo("contacto@acme.com")).thenReturn(false);
        when(empresaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(false);
        when(empresaRepository.existsBySlug("acme-s-a")).thenReturn(false);
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("clave123")).thenReturn("hash-seguro");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new RuntimeException("fallo inesperado al guardar el usuario"));

        assertThatThrownBy(() -> service.registrar(request()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(emailVerificacionService, never()).enviarCorreoVerificacion(any(), any());
    }
}
