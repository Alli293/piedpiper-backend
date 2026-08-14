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
import com.piedpiper.carbonhub.auditoria.models.enums.ResultadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.models.events.AuditoriaFinalizadaEvent;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.auditoria.repository.TransicionEstadoAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.models.dtos.EmitirCertificacionRequestDTO;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.service.EmisionCertificacionPort;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
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
import org.springframework.context.ApplicationEventPublisher;
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

    /** La solicitud de prueba tiene la auditoria realizada el 2026-08-05. */
    private static final LocalDate FECHA_AUDITORIA = LocalDate.of(2026, 8, 5);
    private static final LocalDate VENCIMIENTO_VALIDO = LocalDate.of(2027, 8, 5);
    private static final String OBSERVACIONES_VALIDAS =
            "Falta el desglose de alcance 3 y el respaldo de las facturas electricas.";

    @Mock
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    @Mock
    private TransicionEstadoAuditoriaRepository transicionEstadoAuditoriaRepository;
    @Mock
    private NotificacionTransicionRegistroService notificacionTransicionRegistroService;
    @Mock
    private EmisionCertificacionPort emisionCertificacionPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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
                new CatalogoTiposCertificacion(),
                eventPublisher,
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

        ArgumentCaptor<AuditoriaFinalizadaEvent> eventCaptor =
                ArgumentCaptor.forClass(AuditoriaFinalizadaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().auditoriaId()).isEqualTo(SOLICITUD_ID);
        assertThat(eventCaptor.getValue().auditorId()).isEqualTo(AUDITOR_ID);
    }

    @Test
    void aprobarSinSincronizacionActivaEmiteInmediatamente() {
        service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID);

        verify(emisionCertificacionPort).emitirPorAuditoriaAprobada(any());
        verify(eventPublisher).publishEvent(any(AuditoriaFinalizadaEvent.class));
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

    /**
     * Sin la fecha, la certificacion nace sin vigencia y no hay forma de saber cuando caduca. Es
     * obligatoria solo al aprobar, asi que Bean Validation no puede exigirla desde el DTO.
     */
    @Test
    void aprobarSinFechaDeVencimientoDevuelve422() {
        ResultadoAuditoriaRequestDTO sinFecha =
                new ResultadoAuditoriaRequestDTO("aprobada", null, null);

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, sinFecha, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    /**
     * La comparacion es contra la fecha de la auditoria y no contra hoy: una certificacion que
     * vence antes del trabajo que la sustenta nace invalida.
     */
    @Test
    void aprobarConVencimientoAnteriorALaAuditoriaDevuelve422() {
        ResultadoAuditoriaRequestDTO vencidaAlNacer = new ResultadoAuditoriaRequestDTO(
                "aprobada", null, FECHA_AUDITORIA.minusDays(1));

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, vencidaAlNacer, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /** El mismo dia tampoco sirve: la vigencia tiene que ser posterior, no igual. */
    @Test
    void aprobarConVencimientoElMismoDiaDeLaAuditoriaDevuelve422() {
        ResultadoAuditoriaRequestDTO mismoDia =
                new ResultadoAuditoriaRequestDTO("aprobada", null, FECHA_AUDITORIA);

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, mismoDia, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * El catalogo le da 12 meses de vigencia a Inventario GEI. Sin tope, un error de tipeo del
     * auditor emitiria una certificacion firmada valida por decadas: el catalogo existe justamente
     * para que la vigencia no la decida quien llena el formulario.
     */
    @Test
    void aprobarConVencimientoMasAllaDeLaVigenciaDelCatalogoDevuelve422() {
        ResultadoAuditoriaRequestDTO exagerada = new ResultadoAuditoriaRequestDTO(
                "aprobada", null, LocalDate.of(2099, 8, 5));

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, exagerada, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    /** El limite exacto si entra: 12 meses desde la auditoria es una vigencia valida. */
    @Test
    void aprobarConElVencimientoJustoEnElLimiteDeLaVigenciaSiEntra() {
        ResultadoAuditoriaRequestDTO enElLimite = new ResultadoAuditoriaRequestDTO(
                "aprobada", null, FECHA_AUDITORIA.plusMonths(12));

        service.emitir(SOLICITUD_ID, enElLimite, AUDITOR_ID);

        assertThat(capturarGuardada().getFechaVencimientoCert())
                .isEqualTo(FECHA_AUDITORIA.plusMonths(12));
    }

    /** Un dia mas alla del limite ya no: el tope es el catalogo, no una aproximacion. */
    @Test
    void unDiaDespuesDelLimiteDeVigenciaYaNoEntra() {
        ResultadoAuditoriaRequestDTO pasada = new ResultadoAuditoriaRequestDTO(
                "aprobada", null, FECHA_AUDITORIA.plusMonths(12).plusDays(1));

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, pasada, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /** Acortar la vigencia si es decision del auditor; lo que no puede es estirarla. */
    @Test
    void elAuditorPuedeAcortarLaVigenciaPorDebajoDelMaximo() {
        ResultadoAuditoriaRequestDTO corta = new ResultadoAuditoriaRequestDTO(
                "aprobada", null, FECHA_AUDITORIA.plusMonths(6));

        service.emitir(SOLICITUD_ID, corta, AUDITOR_ID);

        assertThat(capturarGuardada().getFechaVencimientoCert())
                .isEqualTo(FECHA_AUDITORIA.plusMonths(6));
    }

    /**
     * La fecha queda tambien en la solicitud, no solo en el comando de emision: la emision corre
     * despues del commit, asi que si falla, este es el unico lugar donde el dato sobrevive para el
     * reintento manual del administrador.
     */
    @Test
    void aprobarGuardaLaVigenciaEnLaSolicitudYLaEnviaEnLaEmision() {
        service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID);

        assertThat(capturarGuardada().getFechaVencimientoCert()).isEqualTo(VENCIMIENTO_VALIDO);

        ArgumentCaptor<EmitirCertificacionRequestDTO> captor =
                ArgumentCaptor.forClass(EmitirCertificacionRequestDTO.class);
        verify(emisionCertificacionPort).emitirPorAuditoriaAprobada(captor.capture());
        assertThat(captor.getValue().getFechaVencimientoCert()).isEqualTo(VENCIMIENTO_VALIDO);
    }

    @Test
    void observacionesDemasiadoCortasDevuelve422() {
        ResultadoAuditoriaRequestDTO corta =
                new ResultadoAuditoriaRequestDTO("observaciones", "revisar", null);

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, corta, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        verify(solicitudAuditoriaRepository, never()).saveAndFlush(any());
    }

    /**
     * Los espacios no cuentan como contenido: sin recortar, veinte espacios pasarian el minimo y la
     * empresa recibiria una devolucion sin ninguna instruccion.
     */
    @Test
    void observacionesDeSoloEspaciosNoAlcanzanElMinimo() {
        ResultadoAuditoriaRequestDTO enBlanco =
                new ResultadoAuditoriaRequestDTO("observaciones", " ".repeat(40), null);

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, enBlanco, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void observacionesMasLargasQueElMaximoDevuelve422() {
        ResultadoAuditoriaRequestDTO larga = new ResultadoAuditoriaRequestDTO(
                "observaciones", "a".repeat(SolicitudAuditoria.OBSERVACIONES_MAX + 1), null);

        assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, larga, AUDITOR_ID))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void observacionesQuedanGuardadasRecortadasEnLaSolicitud() {
        ResultadoAuditoriaRequestDTO conEspacios = new ResultadoAuditoriaRequestDTO(
                "observaciones", "  " + OBSERVACIONES_VALIDAS + "  ", null);

        service.emitir(SOLICITUD_ID, conEspacios, AUDITOR_ID);

        assertThat(capturarGuardada().getObservaciones()).isEqualTo(OBSERVACIONES_VALIDAS);
    }

    /**
     * La historia nombra al resultado {@code observaciones_pendientes}; el frontend ya desplegado
     * envia {@code observaciones}. Los dos tienen que entrar o una version del cliente deja de
     * poder emitir resultados.
     */
    @Test
    void observacionesPendientesEsElMismoResultadoQueObservaciones() {
        service.emitir(SOLICITUD_ID, datos("observaciones_pendientes"), AUDITOR_ID);

        assertThat(capturarGuardada().getEstado())
                .isEqualTo(EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES);
    }

    /**
     * El resultado se guarda ademas del estado: el estado dice donde quedo la solicitud, el
     * resultado dice que decidio el auditor, y la pantalla muestra las dos cosas.
     */
    @Test
    void elResultadoYLaFechaDeResolucionQuedanEnLaSolicitud() {
        Instant antes = Instant.now();

        service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getResultadoAuditoria()).isEqualTo(ResultadoAuditoria.APROBADA);
        assertThat(guardada.getFechaResolucion()).isNotNull().isAfterOrEqualTo(antes);
    }

    /** Una devolucion con observaciones no emite certificacion, asi que no tiene vigencia. */
    @Test
    void observacionesNoDejanFechaDeVencimiento() {
        service.emitir(SOLICITUD_ID, datos("observaciones"), AUDITOR_ID);

        SolicitudAuditoria guardada = capturarGuardada();
        assertThat(guardada.getResultadoAuditoria()).isEqualTo(ResultadoAuditoria.OBSERVACIONES);
        assertThat(guardada.getFechaVencimientoCert()).isNull();
    }

    /**
     * {@code CERTIFICACION_EMITIDA} y {@code OBSERVACIONES_PENDIENTES} son estados finales: ninguna
     * segunda emision puede cambiar el resultado ya registrado.
     */
    @Test
    void unaSolicitudEnEstadoFinalYaNoAceptaOtroResultado() {
        for (EstadoSolicitudAuditoria estadoFinal : List.of(
                EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA,
                EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES)) {
            SolicitudAuditoria cerrada = solicitud();
            cerrada.setEstado(estadoFinal);
            when(solicitudAuditoriaRepository.findById(SOLICITUD_ID)).thenReturn(Optional.of(cerrada));

            assertThatThrownBy(() -> service.emitir(SOLICITUD_ID, datos("aprobada"), AUDITOR_ID))
                    .as("desde %s no deberia poder emitirse otro resultado", estadoFinal)
                    .isInstanceOf(ApiException.class)
                    .extracting(error -> ((ApiException) error).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
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

    /**
     * Arma la peticion con el campo condicional que ese resultado exige, que es como llega desde el
     * formulario. Los casos que prueban justamente la falta de ese campo lo construyen a mano.
     */
    private static ResultadoAuditoriaRequestDTO datos(String resultado) {
        boolean aprueba = resultado.toLowerCase().startsWith("aprob");
        return new ResultadoAuditoriaRequestDTO(
                resultado,
                aprueba ? null : OBSERVACIONES_VALIDAS,
                aprueba ? VENCIMIENTO_VALIDO : null);
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
                        .sectorIndustrial(SectorIndustrial.AGROINDUSTRIA)
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
