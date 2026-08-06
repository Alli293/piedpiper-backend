package com.piedpiper.carbonhub.auditoria.service;

import com.piedpiper.carbonhub.auditoria.mappers.SolicitudAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.mappers.TransicionEstadoAuditoriaMapperImpl;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.ReporteAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CargaReporteAuditoriaServiceTest {

    private static final UUID SOLICITUD_ID = UUID.fromString("9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f");
    private static final UUID AUDITOR_ID = UUID.fromString("c0ffee00-1111-2222-3333-444455556666");
    private static final UUID OTRO_AUDITOR_ID = UUID.fromString("deadbeef-1111-2222-3333-444455556666");
    private static final byte[] CONTENIDO = "%PDF-1.7 valido".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private ValidadorReporteAuditoriaPdf validadorReporteAuditoriaPdf;
    @Mock
    private ReporteAuditoriaFactory reporteAuditoriaFactory;
    @Mock
    private NotificacionTransicionRegistroService notificacionTransicionRegistroService;

    private CargaReporteAuditoriaService service;

    @BeforeEach
    void configurar() {
        TransicionEstadoAuditoriaService transiciones = new TransicionEstadoAuditoriaService(
                transicionEstadoAuditoriaRepository,
                new ValidadorTransicionAuditoria(),
                notificacionTransicionRegistroService);

        service = new CargaReporteAuditoriaService(
                solicitudAuditoriaRepository,
                transicionEstadoAuditoriaRepository,
                validadorReporteAuditoriaPdf,
                reporteAuditoriaFactory,
                transiciones,
                new SolicitudAuditoriaMapperImpl(),
                new TransicionEstadoAuditoriaMapperImpl());

        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud()));
        when(validadorReporteAuditoriaPdf.validar(any())).thenReturn(CONTENIDO);
        when(reporteAuditoriaFactory.crear(any(), eq(CONTENIDO), any()))
                .thenAnswer(invocacion -> reporte((Instant) invocacion.getArgument(2)));
        when(solicitudAuditoriaRepository.saveAndFlush(any(SolicitudAuditoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(transicionEstadoAuditoriaRepository.findBySolicitudIdOrderByFechaAsc(SOLICITUD_ID))
                .thenReturn(List.of());
    }

    @Test
    void cargaInicialGuardaReporteActualizaFechasYTransicionaAReporteCargado() {
        SolicitudAuditoriaDetalleResponseDTO respuesta = service.cargar(
                SOLICITUD_ID, reportePdf(), LocalDate.now().minusDays(1), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.REPORTE_CARGADO);
        assertThat(guardada.getFechaAuditoriaRealizada()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(guardada.getFechaCargaReporte()).isNotNull();
        assertThat(guardada.getReporteAuditoria().getSolicitud()).isSameAs(guardada);
        assertThat(respuesta.getReporteAuditoria().getNombreArchivo()).isEqualTo("reporte.pdf");

        assertThat(capturarTransiciones()).singleElement()
                .satisfies(transicion -> assertThat(transicion.getEvento())
                        .isEqualTo(EventoTransicionAuditoria.REPORTE_CARGADO));
    }

    @Test
    void reemplazoDeReporteNoCambiaElEstadoNiRegistraNuevaTransicion() {
        SolicitudAuditoria solicitud = solicitud();
        solicitud.setEstado(EstadoSolicitudAuditoria.REPORTE_CARGADO);
        solicitud.reemplazarReporteAuditoria(reporte(Instant.parse("2026-07-28T10:00:00Z")));
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.now().minusDays(1), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getEstado()).isEqualTo(EstadoSolicitudAuditoria.REPORTE_CARGADO);
        assertThat(guardada.getReporteAuditoria().getFechaCarga())
                .isAfter(Instant.parse("2026-07-28T10:00:00Z"));
        verify(transicionEstadoAuditoriaRepository, never()).save(any());
    }

    @Test
    void validaEnOrdenExistenciaAuditorEstadoFormatoYFecha() {
        LocalDate fechaInvalida = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> service.cargar(SOLICITUD_ID, reportePdf(), fechaInvalida, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        InOrder orden = inOrder(solicitudAuditoriaRepository, validadorReporteAuditoriaPdf);
        orden.verify(solicitudAuditoriaRepository).findById(SOLICITUD_ID);
        orden.verify(validadorReporteAuditoriaPdf).validar(any());
        verify(reporteAuditoriaFactory, never()).crear(any(), any(), any());
        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void solicitudInexistenteDevuelve404SinValidarArchivo() {
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.now(), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(validadorReporteAuditoriaPdf, never()).validar(any());
    }

    @Test
    void auditorIncorrectoDevuelve403SinValidarArchivo() {
        assertThatThrownBy(() -> service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.now(), OTRO_AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(validadorReporteAuditoriaPdf, never()).validar(any());
    }

    @Test
    void estadoInvalidoDevuelve409SinValidarArchivo() {
        SolicitudAuditoria solicitud = solicitud();
        solicitud.setEstado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.now(), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(validadorReporteAuditoriaPdf, never()).validar(any());
    }

    @Test
    void pdfInvalidoCortaAntesDeValidarFechaYGuardar() {
        when(validadorReporteAuditoriaPdf.validar(any()))
                .thenThrow(ApiException.reporteAuditoriaNoEsPdf());

        assertThatThrownBy(() -> service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.now(), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(reporteAuditoriaFactory, never()).crear(any(), any(), any());
        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void fechaAnteriorAAceptacionDevuelve422() {
        SolicitudAuditoria solicitud = solicitud();
        solicitud.setFechaAceptacion(Instant.parse("2026-08-06T04:04:00Z"));
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> service.cargar(
                SOLICITUD_ID, reportePdf(), LocalDate.of(2026, 8, 4), AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    @Test
    void aceptaFechaDeAceptacionSegunZonaHorariaDeNegocio() {
        SolicitudAuditoria solicitud = solicitud();
        solicitud.setFechaAceptacion(Instant.parse("2026-08-06T04:04:00Z"));
        when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

        service.cargar(SOLICITUD_ID, reportePdf(), LocalDate.of(2026, 8, 5), AUDITOR_ID);

        assertThat(capturarGuardada().getFechaAuditoriaRealizada())
                .isEqualTo(LocalDate.of(2026, 8, 5));
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

    private static SolicitudAuditoria solicitud() {
        return SolicitudAuditoria.builder()
                .id(SOLICITUD_ID)
                .empresa(Empresa.builder()
                        .id(UUID.fromString("6f2a3c1e-7b45-4f0a-9d81-2f6d5b8c9e01"))
                        .nombreEmpresa("Acme S.A.")
                        .correoCorporativo("contacto@acme.cr")
                        .build())
                .estado(EstadoSolicitudAuditoria.EN_REVISION)
                .auditor(Usuario.builder().id(AUDITOR_ID).nombre("Ana").apellidos("Auditora").build())
                .origenAsignacion(OrigenAsignacion.MANUAL)
                .fechaAsignacion(Instant.now().minusSeconds(3600))
                .fechaAceptacion(Instant.now().minusSeconds(5 * 24 * 3600))
                .build();
    }

    private static ReporteAuditoria reporte(Instant fechaCarga) {
        return ReporteAuditoria.builder()
                .nombreArchivo("reporte.pdf")
                .tipoContenido(MediaType.APPLICATION_PDF_VALUE)
                .tamanioBytes(CONTENIDO.length)
                .contenido(CONTENIDO)
                .fechaCarga(fechaCarga)
                .build();
    }

    private static MockMultipartFile reportePdf() {
        return new MockMultipartFile(
                "reporteAuditoria", "reporte.pdf", MediaType.APPLICATION_PDF_VALUE, CONTENIDO);
    }
}
