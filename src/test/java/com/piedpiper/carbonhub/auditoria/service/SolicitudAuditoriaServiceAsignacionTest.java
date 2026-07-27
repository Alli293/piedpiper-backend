package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.AsignarAuditorRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudAuditoriaServiceAsignacionTest {

    private static final UUID USUARIO_ID = UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID OTRA_EMPRESA_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID OTRO_AUDITOR_ID = UUID.fromString("deadbeef-1111-2222-3333-444455556666");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CertificacionActivaConsulta certificacionActivaConsulta;
    @Mock
    private EnvioCorreoAsignacionAuditorService envioCorreoAsignacionAuditorService;

    private SolicitudAuditoriaService service;

    @BeforeEach
    void configurar() {
        service = new SolicitudAuditoriaService(
                solicitudAuditoriaRepository,
                usuarioRepository,
                certificacionActivaConsulta,
                new ValidadorDocumentosPdf(),
                new SolicitudAuditoriaMapperImpl(),
                envioCorreoAsignacionAuditorService);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(administrador(EMPRESA_ID)));
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(auditorCertificadoActivo()));
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud(EMPRESA_ID)));
        when(solicitudAuditoriaRepository.save(any(SolicitudAuditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void asignacionManualGuardaAuditorOrigenYFechaSinModificarElEstado() {
        SolicitudAuditoriaResponseDTO respuesta =
                service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getAuditor().getId()).isEqualTo(AUDITOR_ID);
        assertThat(guardada.getOrigenAsignacion()).isEqualTo(OrigenAsignacion.MANUAL);
        assertThat(guardada.getFechaAsignacion()).isNotNull();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);

        assertThat(respuesta.getIdAuditor()).isEqualTo(AUDITOR_ID);
        assertThat(respuesta.getOrigenAsignacion()).isEqualTo(OrigenAsignacion.MANUAL);
        assertThat(respuesta.getFechaAsignacion()).isNotNull();
        assertThat(respuesta.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    @Test
    void asignacionDesdeRecomendacionIaGuardaElOrigenCorrespondiente() {
        SolicitudAuditoriaResponseDTO respuesta =
                service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "recomendacion_ia"), USUARIO_ID);

        assertThat(capturarGuardada().getOrigenAsignacion()).isEqualTo(OrigenAsignacion.RECOMENDACION_IA);
        assertThat(respuesta.getOrigenAsignacion()).isEqualTo(OrigenAsignacion.RECOMENDACION_IA);
    }

    @Test
    void solicitudInexistenteDevuelve404() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Esta solicitud de auditoría no fue encontrada.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(solicitudAuditoriaRepository, never()).save(any());
        verifyNoInteractions(envioCorreoAsignacionAuditorService);
    }

    @Test
    void solicitudDeOtraEmpresaDevuelve403SinRevelarSiExiste() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitud(OTRA_EMPRESA_ID)));

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("No tienes permiso para gestionar esta solicitud de auditoría.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void solicitudYaAsignadaAOtroAuditorDevuelve409() {
        SolicitudAuditoria yaAsignada = solicitud(EMPRESA_ID);
        yaAsignada.setAuditor(Usuario.builder().id(OTRO_AUDITOR_ID).build());
        yaAsignada.setFechaAsignacion(Instant.now());
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(yaAsignada));

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe una solicitud de revisión pendiente con otro auditor.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void auditorInexistenteDevuelve422() {
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Este auditor no está disponible actualmente.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoEsAuditorCertificadoDevuelve422() {
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(AUDITOR_ID)
                .rol(Rol.USUARIO_GENERAL)
                .estado(EstadoUsuario.ACTIVO)
                .build()));

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Este auditor no está disponible actualmente.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void auditorCertificadoQueNoEstaActivoDevuelve422() {
        when(usuarioRepository.findById(AUDITOR_ID)).thenReturn(Optional.of(Usuario.builder()
                .id(AUDITOR_ID)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.PENDIENTE_VALIDACION)
                .build()));

        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("Este auditor no está disponible actualmente.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void origenDeAsignacionFueraDelCatalogoDevuelve422() {
        assertThatThrownBy(() -> service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "sorteo"), USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .hasMessage("El origen de la asignación debe ser 'manual' o 'recomendacion_ia'.")
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).save(any());
        verifyNoInteractions(envioCorreoAsignacionAuditorService);
    }

    @Test
    void laNotificacionAlAuditorSeEnviaSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID);

        verifyNoInteractions(envioCorreoAsignacionAuditorService);

        List<TransactionSynchronization> sincronizaciones =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        assertThat(sincronizaciones).hasSize(1);
        sincronizaciones.forEach(TransactionSynchronization::afterCommit);

        verify(envioCorreoAsignacionAuditorService)
                .enviarAsignacion("Ana Auditora", "auditora@carbonhub.cr", "Empresa Demo");
    }

    @Test
    void unFalloDeEnvioDeCorreoNoRevierteLaAsignacion() {
        doThrow(new IllegalStateException("SMTP caido"))
                .when(envioCorreoAsignacionAuditorService).enviarAsignacion(anyString(), anyString(), anyString());
        TransactionSynchronizationManager.initSynchronization();

        SolicitudAuditoriaResponseDTO respuesta =
                service.asignarAuditor(SOLICITUD_ID, datos(AUDITOR_ID, "manual"), USUARIO_ID);
        SolicitudAuditoria guardada = capturarGuardada();

        List<TransactionSynchronization> sincronizaciones =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        assertThatThrownBy(() -> sincronizaciones.forEach(TransactionSynchronization::afterCommit))
                .isInstanceOf(IllegalStateException.class);

        assertThat(respuesta.getIdAuditor()).isEqualTo(AUDITOR_ID);
        assertThat(guardada.getAuditor().getId()).isEqualTo(AUDITOR_ID);
        assertThat(guardada.getOrigenAsignacion()).isEqualTo(OrigenAsignacion.MANUAL);
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA);
    }

    @Test
    void obtenerDevuelveLaSolicitudConLaAsignacionVigente() {
        SolicitudAuditoria yaAsignada = solicitud(EMPRESA_ID);
        yaAsignada.setAuditor(auditorCertificadoActivo());
        yaAsignada.setOrigenAsignacion(OrigenAsignacion.MANUAL);
        yaAsignada.setFechaAsignacion(Instant.now());
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(yaAsignada));

        SolicitudAuditoriaResponseDTO respuesta = service.obtener(SOLICITUD_ID, USUARIO_ID);

        assertThat(respuesta.getIdAuditor()).isEqualTo(AUDITOR_ID);
        assertThat(respuesta.getOrigenAsignacion()).isEqualTo(OrigenAsignacion.MANUAL);
        assertThat(respuesta.getFechaAsignacion()).isNotNull();
        assertThat(respuesta.getAuditor()).isNotNull();
        assertThat(respuesta.getAuditor().getId()).isEqualTo(AUDITOR_ID);
        assertThat(respuesta.getAuditor().getNombre()).isEqualTo("Ana Auditora");
        verify(solicitudAuditoriaRepository, never()).save(any());
    }

    @Test
    void obtenerSolicitudInexistenteDevuelve404() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(SOLICITUD_ID, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void obtenerSolicitudDeOtraEmpresaDevuelve403() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID))
                .thenReturn(Optional.of(solicitud(OTRA_EMPRESA_ID)));

        assertThatThrownBy(() -> service.obtener(SOLICITUD_ID, USUARIO_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private SolicitudAuditoria capturarGuardada() {
        ArgumentCaptor<SolicitudAuditoria> captor = ArgumentCaptor.forClass(SolicitudAuditoria.class);
        verify(solicitudAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static AsignarAuditorRequestDTO datos(UUID idAuditor, String origenAsignacion) {
        return new AsignarAuditorRequestDTO(idAuditor, origenAsignacion);
    }

    private static SolicitudAuditoria solicitud(UUID empresaId) {
        return SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .id(empresaId)
                        .nombreEmpresa("Empresa Demo")
                        .correoCorporativo("contacto@empresa.cr")
                        .build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.now().minusMonths(6))
                .periodoFin(LocalDate.now().minusMonths(1))
                .estado(EstadoSolicitudAuditoria.SOLICITUD_ENVIADA)
                .fechaCreacion(Instant.now())
                .build();
    }

    private static Usuario administrador(UUID empresaId) {
        return Usuario.builder()
                .id(USUARIO_ID)
                .rol(Rol.ADMINISTRADOR_EMPRESA)
                .estado(EstadoUsuario.ACTIVO)
                .empresa(Empresa.builder().id(empresaId).nombreEmpresa("Empresa Demo").build())
                .build();
    }

    private static Usuario auditorCertificadoActivo() {
        return Usuario.builder()
                .id(AUDITOR_ID)
                .nombreVisible("Ana Auditora")
                .email("auditora@carbonhub.cr")
                .rol(Rol.AUDITOR_CERTIFICADO)
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }
}
