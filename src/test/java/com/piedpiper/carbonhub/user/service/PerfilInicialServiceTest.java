package com.piedpiper.carbonhub.user.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.dtos.DatosEmpresaPerfilDTO;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialRequestDTO;
import com.piedpiper.carbonhub.user.models.dtos.PerfilInicialResponseDTO;
import com.piedpiper.carbonhub.user.models.dtos.PreferenciasUsuarioRequestDTO;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilInicialServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private PerfilInicialService service;

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private Usuario usuario(Rol rol) {
        return Usuario.builder()
                .id(USUARIO_ID)
                .email("usuario@correo.com")
                .nombre("Ana")
                .apellidos("Gómez")
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .metodoAuth(MetodoAuth.CORREO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private Empresa empresa() {
        return Empresa.builder()
                .id(UUID.randomUUID())
                .nombreEmpresa("Café del Valle S.A.")
                .cedulaJuridica("3-101-123456")
                .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
                .pais("Costa Rica")
                .cantidadEmpleados(10)
                .correoCorporativo("empresa@correo.com")
                .slug("cafe-del-valle-s-a")
                .estado(com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private PerfilInicialRequestDTO requestBasico() {
        return new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("ESPANOL", "CRC", "METRICO"), null);
    }

    @Test
    void usuarioIndividual_persisteNombreYPreferenciasYMarcaCompletado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO_INDIVIDUAL)));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        PerfilInicialResponseDTO response = service.completar(USUARIO_ID, requestBasico());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getNombreVisible()).isEqualTo("Ana G.");
        assertThat(guardado.getIdioma()).isEqualTo("ESPANOL");
        assertThat(guardado.isConfiguracionCompleta()).isTrue();

        assertThat(response.isConfiguracionCompleta()).isTrue();
        assertThat(response.getRedirect()).isEqualTo("/ecoruta/preferencias");
        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @Test
    void adminEmpresa_editaDatosDeEmpresaValidosYSePersisten() {
        Usuario admin = usuario(Rol.ADMINISTRADOR_EMPRESA);
        admin.setEmpresa(empresa());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("INGLES", "USD", "METRICO"),
                new DatosEmpresaPerfilDTO("SERVICIOS", "Panamá", 25));

        PerfilInicialResponseDTO response = service.completar(USUARIO_ID, request);

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).saveAndFlush(captor.capture());
        Empresa guardada = captor.getValue();
        assertThat(guardada.getSectorIndustrial()).isEqualTo(SectorIndustrial.SERVICIOS);
        assertThat(guardada.getPais()).isEqualTo("Panamá");
        assertThat(guardada.getCantidadEmpleados()).isEqualTo(25);

        assertThat(response.getRedirect()).isEqualTo("/empresa/panel");
        assertThat(response.getEmpresa().getSectorIndustrial()).isEqualTo("SERVICIOS");
    }

    @Test
    void usuarioGeneral_intentaModificarEmpresa_rechazaCon403SinPersistir() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO_GENERAL)));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("ESPANOL", "CRC", "METRICO"),
                new DatosEmpresaPerfilDTO("SERVICIOS", "Panamá", 25));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @Test
    void preferenciaFueraDeCatalogo_rechazaCon422SinMarcarCompletado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO_INDIVIDUAL)));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("FRANCES", "CRC", "METRICO"), null);

        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void sectorFueraDeCatalogo_rechazaCon422() {
        Usuario admin = usuario(Rol.ADMINISTRADOR_EMPRESA);
        admin.setEmpresa(empresa());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("ESPANOL", "CRC", "METRICO"),
                new DatosEmpresaPerfilDTO("MINERIA", "Costa Rica", 25));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @Test
    void adminSinEmpresaAsociada_rechazaCon403() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.ADMINISTRADOR_EMPRESA)));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("ESPANOL", "CRC", "METRICO"),
                new DatosEmpresaPerfilDTO("SERVICIOS", "Panamá", 25));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void adminSinEmpresa_soloPreferencias_rechazaCon403SinMarcarCompletado() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.ADMINISTRADOR_EMPRESA)));

        PerfilInicialRequestDTO request = requestBasico();
        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("empresa")
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }

    @Test
    void fallaDePersistencia_lanza500ConMensajeDeReintento() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO_INDIVIDUAL)));
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        PerfilInicialRequestDTO request = requestBasico();
        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("No se pudo guardar tu perfil. Intenta nuevamente.")
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void obtener_usuarioGeneralConEmpresa_incluyeDatosDeEmpresaParaSoloLectura() {
        Usuario general = usuario(Rol.USUARIO_GENERAL);
        general.setEmpresa(empresa());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(general));

        PerfilInicialResponseDTO response = service.obtener(USUARIO_ID);

        assertThat(response.getRol()).isEqualTo("USUARIO_GENERAL");
        assertThat(response.isConfiguracionCompleta()).isFalse();
        assertThat(response.getEmpresa()).isNotNull();
        assertThat(response.getEmpresa().getNombreEmpresa()).isEqualTo("Café del Valle S.A.");
        assertThat(response.getNombreVisible()).isEqualTo("Ana");
        assertThat(response.getNombre()).isEqualTo("Ana");
        assertThat(response.getApellidos()).isEqualTo("Gómez");
        assertThat(response.getPreferencias().getIdioma()).isEqualTo("ESPANOL");
    }

    @Test
    void obtener_perfilIncompleto_redirigeALaConfiguracionInicial() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO_INDIVIDUAL)));

        PerfilInicialResponseDTO response = service.obtener(USUARIO_ID);

        assertThat(response.getRedirect()).isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void obtener_adminConEmpresaPeroPerfilIncompleto_redirigeALaConfiguracionInicialDelPerfil() {
        Usuario admin = usuario(Rol.ADMINISTRADOR_EMPRESA);
        admin.setEmpresa(empresa());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));

        PerfilInicialResponseDTO response = service.obtener(USUARIO_ID);

        assertThat(response.isConfiguracionCompleta()).isFalse();
        assertThat(response.getRedirect()).isEqualTo("/perfil/configuracion-inicial");
    }

    @Test
    void usuarioNoEncontrado_rechazaCon403() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void variosValoresInvalidos_reportaTodosLosErroresEnUnSolo422() {
        Usuario admin = usuario(Rol.ADMINISTRADOR_EMPRESA);
        admin.setEmpresa(empresa());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));

        PerfilInicialRequestDTO request = new PerfilInicialRequestDTO("Ana G.",
                new PreferenciasUsuarioRequestDTO("FRANCES", "EUR", "IMPERIAL"),
                new DatosEmpresaPerfilDTO("MINERIA", "Costa Rica", 25));

        assertThatThrownBy(() -> service.completar(USUARIO_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("idioma")
                .hasMessageContaining("moneda")
                .hasMessageContaining("unidades")
                .hasMessageContaining("sector")
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verify(empresaRepository, never()).saveAndFlush(any(Empresa.class));
    }
}
