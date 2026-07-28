package com.piedpiper.carbonhub.certificacion.service;

import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.mappers.CertificacionMapper;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.IndiceEstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.NotificacionPanel;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.IndiceEstadoCertificacionRepository;
import com.piedpiper.carbonhub.certificacion.repository.NotificacionPanelRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionCertificacionServiceTest {

    private static final UUID ID_AUDITORIA = UUID.randomUUID();
    private static final UUID ID_EMPRESA = UUID.randomUUID();
    private static final UUID ID_AUDITOR = UUID.randomUUID();
    private static final LocalDate FECHA_AUDITORIA = LocalDate.of(2026, 1, 10);

    @Mock
    private CertificacionRepository certificacionRepository;
    @Mock
    private NotificacionPanelRepository notificacionPanelRepository;
    @Mock
    private IndiceEstadoCertificacionRepository indiceEstadoCertificacionRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private GeneradorCredencialOpenBadges generadorCredencialOpenBadges;
    @Mock
    private CertificacionMapper certificacionMapper;
    @Mock
    private CertificacionPersistenciaService certificacionPersistenciaService;

    private final CatalogoTiposCertificacion catalogo = new CatalogoTiposCertificacion();

    private EmisionCertificacionService service;

    @BeforeEach
    void prepararServicio() {
        // El catalogo es la instancia real: es datos de referencia, no colaborador.
        service = new EmisionCertificacionService(certificacionRepository,
                notificacionPanelRepository, indiceEstadoCertificacionRepository,
                empresaRepository, usuarioRepository,
                catalogo, generadorCredencialOpenBadges, certificacionMapper,
                certificacionPersistenciaService);
    }

    private EmisionCertificacionService service() {
        return service;
    }

    private EmitirCertificacionRequestDTO comando(TipoCertificacion tipo) {
        return new EmitirCertificacionRequestDTO(ID_AUDITORIA, ID_EMPRESA, ID_AUDITOR,
                "aprobada", FECHA_AUDITORIA, tipo, null);
    }

    private Usuario auditorValido() {
        return Usuario.builder().id(ID_AUDITOR)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    /** Auditoria sin certificar y empresa/auditor existentes. */
    private void mockearEntidadesResueltas() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR))
                .thenReturn(Optional.of(auditorValido()));
    }

    private void mockearEmisionExitosa() {
        mockearEntidadesResueltas();
        mockearIndiceEstado();
        when(generadorCredencialOpenBadges.generar(any(), any())).thenReturn("jwt.firmado.aqui");
        when(certificacionPersistenciaService.guardar(any(Certificacion.class)))
                .thenAnswer(i -> i.getArgument(0));
        mockearMapperComoIdentidad();
    }

    /** El indice es irrelevante para estas pruebas; solo debe existir alguno. */
    private void mockearIndiceEstado() {
        when(indiceEstadoCertificacionRepository.save(any(IndiceEstadoCertificacion.class)))
                .thenReturn(new IndiceEstadoCertificacion(1L));
    }

    private void mockearMapperComoIdentidad() {
        when(certificacionMapper.toDto(any(Certificacion.class))).thenAnswer(invocation -> {
            Certificacion certificacion = invocation.getArgument(0);
            CertificacionResponseDTO dto = new CertificacionResponseDTO();
            dto.setId(certificacion.getId());
            dto.setIdAuditoria(certificacion.getIdAuditoria());
            dto.setFechaVencimiento(certificacion.getFechaVencimiento());
            dto.setCredencialJwt(certificacion.getCredencialJwt());
            dto.setEstado(certificacion.getEstado() == null
                    ? null : certificacion.getEstado().name());
            return dto;
        });
    }

    private Certificacion capturarGuardada() {
        ArgumentCaptor<Certificacion> captor = ArgumentCaptor.forClass(Certificacion.class);
        verify(certificacionPersistenciaService).guardar(captor.capture());
        return captor.getValue();
    }

    @Test
    void emiteLaCertificacionYRegistraLaNotificacionDelPanel() {
        mockearEmisionExitosa();

        CertificacionResponseDTO response =
                service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL));

        Certificacion guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoCertificacion.ACTIVA);
        assertThat(guardada.getCredencialJwt()).isEqualTo("jwt.firmado.aqui");
        assertThat(guardada.getTipo()).isEqualTo(TipoCertificacion.CARBONO_NEUTRAL);
        verify(notificacionPanelRepository).save(any(NotificacionPanel.class));
        assertThat(response.isRecienEmitida()).isTrue();
        assertThat(response.getNombreCertificacion()).isEqualTo("Carbono Neutral");
    }

    @Test
    void derivaLaFechaDeVencimientoDeLaVigenciaDelTipo() {
        mockearEmisionExitosa();

        service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(capturarGuardada().getFechaVencimiento())
                .isEqualTo(FECHA_AUDITORIA.plusMonths(12));
    }

    @Test
    void marcaVigenteVerdaderoCuandoLaFechaDeVencimientoNoHaPasado() {
        mockearEmisionExitosa();

        CertificacionResponseDTO response =
                service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(response.isVigente()).isTrue();
    }

    @Test
    void marcaVigenteFalsoCuandoLaFechaDeVencimientoYaPaso() {
        mockearEmisionExitosa();
        EmitirCertificacionRequestDTO comando = comando(TipoCertificacion.CARBONO_NEUTRAL);
        comando.setFechaVencimientoCert(FECHA_AUDITORIA.plusDays(30));

        CertificacionResponseDTO response = service().emitirPorAuditoriaAprobada(comando);

        assertThat(response.isVigente()).isFalse();
    }

    @Test
    void usaLaVigenciaDeTreintaYSeisMesesParaHuellaDeProducto() {
        mockearEmisionExitosa();

        service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.HUELLA_PRODUCTO));

        assertThat(capturarGuardada().getFechaVencimiento())
                .isEqualTo(FECHA_AUDITORIA.plusMonths(36));
    }

    @Test
    void usaLaVigenciaDeVeinticuatroMesesParaAdaptacionClimatica() {
        mockearEmisionExitosa();

        service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.ADAPTACION_CLIMATICA));

        assertThat(capturarGuardada().getFechaVencimiento())
                .isEqualTo(FECHA_AUDITORIA.plusMonths(24));
    }

    @Test
    void respetaLaFechaDeVencimientoExplicitaCuandoEsPosteriorALaAuditoria() {
        mockearEmisionExitosa();
        EmitirCertificacionRequestDTO comando = comando(TipoCertificacion.CARBONO_NEUTRAL);
        comando.setFechaVencimientoCert(FECHA_AUDITORIA.plusDays(30));

        service().emitirPorAuditoriaAprobada(comando);

        assertThat(capturarGuardada().getFechaVencimiento())
                .isEqualTo(FECHA_AUDITORIA.plusDays(30));
    }

    @Test
    void devuelveLaCertificacionExistenteSinDuplicarCuandoLaAuditoriaYaFueCertificada() {
        Certificacion existente = Certificacion.builder()
                .id(UUID.randomUUID())
                .idAuditoria(ID_AUDITORIA)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(FECHA_AUDITORIA.plusMonths(12))
                .build();
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA))
                .thenReturn(Optional.of(existente));
        mockearMapperComoIdentidad();

        CertificacionResponseDTO response =
                service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(response.isRecienEmitida()).isFalse();
        assertThat(response.getIdAuditoria()).isEqualTo(ID_AUDITORIA);
        verify(certificacionPersistenciaService, never()).guardar(any());
        verify(notificacionPanelRepository, never()).save(any());
        verify(generadorCredencialOpenBadges, never()).generar(any(), any());
    }

    @Test
    void anteEmisionConcurrenteReutilizaLaCertificacionYaPersistida() {
        Certificacion ganadora = Certificacion.builder()
                .id(UUID.randomUUID())
                .idAuditoria(ID_AUDITORIA)
                .tipo(TipoCertificacion.CARBONO_NEUTRAL)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(FECHA_AUDITORIA.plusMonths(12))
                .build();
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR))
                .thenReturn(Optional.of(auditorValido()));
        mockearIndiceEstado();
        when(generadorCredencialOpenBadges.generar(any(), any())).thenReturn("jwt.firmado.aqui");
        when(certificacionPersistenciaService.guardar(any(Certificacion.class)))
                .thenThrow(new DataIntegrityViolationException("id_auditoria duplicado"));
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA))
                .thenReturn(Optional.empty(), Optional.of(ganadora));
        mockearMapperComoIdentidad();

        CertificacionResponseDTO response =
                service().emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL));

        assertThat(response.isRecienEmitida()).isFalse();
        verify(notificacionPanelRepository, never()).save(any());
    }

    @Test
    void rechazaUnResultadoDistintoDeAprobadaSinConsultarNada() {
        EmitirCertificacionRequestDTO comando = comando(TipoCertificacion.CARBONO_NEUTRAL);
        comando.setResultadoAuditoria("rechazada");

        assertThatThrownBy(() -> service().emitirPorAuditoriaAprobada(comando))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(certificacionRepository, never()).findByIdAuditoria(any());
        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void rechazaLaFechaDeVencimientoIgualALaDeLaAuditoria() {
        mockearEntidadesResueltas();
        EmitirCertificacionRequestDTO comando = comando(TipoCertificacion.CARBONO_NEUTRAL);
        comando.setFechaVencimientoCert(FECHA_AUDITORIA);

        assertThatThrownBy(() -> service().emitirPorAuditoriaAprobada(comando))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void rechazaLaFechaDeVencimientoAnteriorALaDeLaAuditoria() {
        mockearEntidadesResueltas();
        EmitirCertificacionRequestDTO comando = comando(TipoCertificacion.CARBONO_NEUTRAL);
        comando.setFechaVencimientoCert(FECHA_AUDITORIA.minusDays(1));

        assertThatThrownBy(() -> service().emitirPorAuditoriaAprobada(comando))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void noEmiteSiLaEmpresaNoExiste() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service()
                .emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(generadorCredencialOpenBadges, never()).generar(any(), any());
        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void noEmiteSiElAuditorNoExiste() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service()
                .emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(generadorCredencialOpenBadges, never()).generar(any(), any());
        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void noEmiteSiElUsuarioNoTieneRolDeAuditor() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR)).thenReturn(Optional.of(
                Usuario.builder().id(ID_AUDITOR)
                        .rol(Rol.USUARIO_GENERAL)
                        .estado(EstadoUsuario.ACTIVO)
                        .build()));

        assertThatThrownBy(() -> service()
                .emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(generadorCredencialOpenBadges, never()).generar(any(), any());
        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void noEmiteSiElAuditorNoEstaActivo() {
        when(certificacionRepository.findByIdAuditoria(ID_AUDITORIA)).thenReturn(Optional.empty());
        when(empresaRepository.findById(ID_EMPRESA))
                .thenReturn(Optional.of(Empresa.builder().id(ID_EMPRESA).build()));
        when(usuarioRepository.findById(ID_AUDITOR)).thenReturn(Optional.of(
                Usuario.builder().id(ID_AUDITOR)
                        .rol(Rol.AUDITOR_CERTIFICADO)
                        .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                        .build()));

        assertThatThrownBy(() -> service()
                .emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(generadorCredencialOpenBadges, never()).generar(any(), any());
        verify(certificacionPersistenciaService, never()).guardar(any());
    }

    @Test
    void siFallaElGuardadoNoSePersisteLaNotificacion() {
        mockearEntidadesResueltas();
        mockearIndiceEstado();
        when(generadorCredencialOpenBadges.generar(any(), any())).thenReturn("jwt.firmado.aqui");
        when(certificacionPersistenciaService.guardar(any(Certificacion.class)))
                .thenThrow(new IllegalStateException("fallo de base de datos"));

        assertThatThrownBy(() -> service()
                .emitirPorAuditoriaAprobada(comando(TipoCertificacion.CARBONO_NEUTRAL)))
                .isInstanceOf(IllegalStateException.class);

        verify(notificacionPanelRepository, never()).save(any());
    }
}
