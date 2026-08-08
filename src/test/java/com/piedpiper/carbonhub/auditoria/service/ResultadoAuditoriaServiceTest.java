package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.ResultadoAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.ReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

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
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultadoAuditoriaServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID EMPRESA_ID = UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID OTRO_AUDITOR_ID = UUID.fromString("deadbeef-1111-2222-3333-444455556666");

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private NotificacionTransicionRegistroService notificacionTransicionRegistroService;
    @Mock
    private EmisionCertificacionPort emisionCertificacionPort;

    private ResultadoAuditoriaService service;

    @BeforeEach
    void configurar() {
        limpiarSincronizacion();

        TransicionEstadoAuditoriaService transiciones = new TransicionEstadoAuditoriaService(
                transicionEstadoAuditoriaRepository,
                new ValidadorTransicionAuditoria(),
                notificacionTransicionRegistroService);

        service = new ResultadoAuditoriaService(
                solicitudAuditoriaRepository,
                transicionEstadoAuditoriaRepository,
                transiciones,
                emisionCertificacionPort,
                new SolicitudAuditoriaMapperImpl(),
                new TransicionEstadoAuditoriaMapperImpl());

        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud()));
        when(solicitudAuditoriaRepository.saveAndFlush(any(SolicitudAuditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(SOLICITUD_ID))
                .thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        limpiarSincronizacion();
    }

    @Test
    void aprobarTransicionaYProgramaEmisionDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
        assertThat(capturarTransiciones()).singleElement()
                .satisfies(transicion -> assertThat(transicion.getEvento())
                        .isEqualTo(EventoTransicionAuditoria.RESULTADO_APROBADA));
        verify(emisionCertificacionPort, never()).emitirPorAuditoriaAprobada(any());

        List<TransactionSynchronization> sincronizaciones =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        assertThat(sincronizaciones).hasSize(1);
        sincronizaciones.getFirst().afterCommit();

        ArgumentCaptor<EmitirCertificacionRequestDTO> captor =
                ArgumentCaptor.forClass(EmitirCertificacionRequestDTO.class);
        verify(emisionCertificacionPort).emitirPorAuditoriaAprobada(captor.capture());
        assertThat(captor.getValue().getIdAuditoria()).isEqualTo(SOLICITUD_ID);
        assertThat(captor.getValue().getIdEmpresa()).isEqualTo(EMPRESA_ID);
        assertThat(captor.getValue().getIdAuditor()).isEqualTo(AUDITOR_ID);
        assertThat(captor.getValue().getFechaAuditoria()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void aprobarSinSincronizacionActivaEmiteInmediatamente() {
        service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID);

        verify(emisionCertificacionPort).emitirPorAuditoriaAprobada(any());
    }

    @Test
    void observacionesTransicionaSinEmitirCertificacion() {
        service.emitir(SOLICITUD_ID, datos("observaciones"), AUDITOR_ID);

        assertThat(capturarGuardada().getEstado())
                .isEqualTo(EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES);
        assertThat(capturarTransiciones()).singleElement()
                .satisfies(transicion -> assertThat(transicion.getEvento())
                        .isEqualTo(EventoTransicionAuditoria.RESULTADO_OBSERVACIONES));
        verify(emisionCertificacionPort, never()).emitirPorAuditoriaAprobada(any());
    }

    @Test
    void resultadoInvalidoDevuelve422() {
        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, datos("quizas"), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void auditorDistintoDevuelve403() {
        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, datos("aprobada"), OTRO_AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void estadoSinReporteCargadoDevuelve409() {
        SolicitudAuditoria solicitud = solicitud();
        solicitud.setEstado(EstadoSolicitudAuditoria.EN_REVISION);
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    private SolicitudAuditoria capturarGuardada() {
        ArgumentCaptor<SolicitudAuditoria> captor = ArgumentCaptor.forClass(SolicitudAuditoria.class);
        verify(solicitudAuditoriaRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private List<TransicionEstadoAuditoria> capturarTransiciones() {
        ArgumentCaptor<TransicionEstadoAuditoria> captor =
                ArgumentCaptor.forClass(TransicionEstadoAuditoria.class);
        verify(transicionEstadoAuditoriaRepository).save(captor.capture());
        return captor.getAllValues();
    }

    private static ResultadoAuditoriaRequestDTO datos(String resultado) {
        return new ResultadoAuditoriaRequestDTO(resultado);
    }

    private static void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static SolicitudAuditoria solicitud() {
        SolicitudAuditoria solicitud = SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .id(EMPRESA_ID)
                        .nombreEmpresa("Acme S.A.")
                        .correoCorporativo("contacto@acme.cr")
                        .build())
                .tipoCertificacion(TipoCertificacionSolicitud.INICIAL)
                .periodoInicio(LocalDate.of(2026, 1, 1))
                .periodoFin(LocalDate.of(2026, 12, 31))
                .estado(EstadoSolicitudAuditoria.REPORTE_CARGADO)
                .fechaCreacion(Instant.parse("2026-08-01T10:00:00Z"))
                .auditor(Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build())
                .origenAsignacion(OrigenAsignacion.MANUAL)
                .fechaAsignacion(Instant.parse("2026-08-01T11:00:00Z"))
                .fechaAceptacion(Instant.parse("2026-08-02T11:00:00Z"))
                .fechaAuditoriaRealizada(LocalDate.of(2026, 8, 5))
                .fechaCargaReporte(Instant.parse("2026-08-05T18:00:00Z"))
                .build();
        ReporteAuditoria reporte = ReporteAuditoria.builder()
                .nombreArchivo("reporte.pdf")
                .tipoContenido(MediaType.APPLICATION_PDF_VALUE)
                .tamanioBytes(128)
                .fechaCarga(Instant.parse("2026-08-05T18:00:00Z"))
                .build();
        solicitud.reemplazarReporteAuditoria(reporte);
        return solicitud;
    }
}
