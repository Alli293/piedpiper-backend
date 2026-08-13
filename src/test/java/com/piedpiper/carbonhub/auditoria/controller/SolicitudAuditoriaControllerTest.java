package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.dtos.AuditorAsignadoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.FiltrarSolicitudesAuditoriaRequestDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.PaginaSolicitudesAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.TransicionEstadoAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EventoTransicionAuditoria;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaListadoService;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaDetalleService;
import com.piedpiper.carbonhub.auditoria.models.enums.OrigenAsignacion;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.service.CargaReporteAuditoriaService;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaService;
import com.piedpiper.carbonhub.auditoria.service.ValidadorDocumentosPdf;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SolicitudAuditoriaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(SolicitudAuditoriaControllerTest.MethodSecurityTestConfig.class)
class SolicitudAuditoriaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String SOLICITUD_ID = "9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f";
    private static final String AUDITOR_ID = "c0ffee00-1111-2222-3333-444455556666";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudAuditoriaService solicitudAuditoriaService;
    @MockitoBean
    private SolicitudAuditoriaDetalleService solicitudAuditoriaDetalleService;
    @MockitoBean
    private SolicitudAuditoriaListadoService solicitudAuditoriaListadoService;
    @MockitoBean
    private CargaReporteAuditoriaService cargaReporteAuditoriaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postValidoDevuelve201ConLaSolicitudCreada() throws Exception {
        when(solicitudAuditoriaService.crear(any(), any(), any())).thenReturn(respuesta());

        mockMvc.perform(multipart("/api/auditorias")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITUD_ENVIADA"))
                .andExpect(jsonPath("$.tipoCertificacion").value("INICIAL"))
                .andExpect(jsonPath("$.documentos[0].nombreArchivo").value("respaldo.pdf"))
                .andExpect(jsonPath("$.documentos[0].contenido").doesNotExist());

        verify(solicitudAuditoriaService).crear(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postSinLaParteDocumentosDevuelve400() throws Exception {
        when(solicitudAuditoriaService.crear(any(), any(), any()))
                .thenThrow(ApiException.documentosRespaldoRequeridos());

        mockMvc.perform(multipart("/api/auditorias")
                        .file(datos())
                        .principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debes adjuntar al menos un documento de respaldo."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postSinFechasDelPeriodoDevuelve400() throws Exception {
        MockMultipartFile datosVacios = new MockMultipartFile("datos", "datos.json",
                MediaType.APPLICATION_JSON_VALUE, "{}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/auditorias")
                        .file(datosVacios)
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void unAdjuntoQueExcedeElLimiteDeMultipartDevuelve400YNo500() throws Exception {
        when(solicitudAuditoriaService.crear(any(), any(), any()))
                .thenThrow(new MaxUploadSizeExceededException(ValidadorDocumentosPdf.TAMANIO_MAXIMO_BYTES));

        mockMvc.perform(multipart("/api/auditorias")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El archivo no puede superar 15 MB."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(multipart("/api/auditorias")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postDeAsignacionValidoDevuelve200ConLaSolicitudActualizada() throws Exception {
        when(solicitudAuditoriaService.asignarAuditor(any(), any(), any())).thenReturn(respuestaAsignada());

        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("manual"))
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAuditor").value(AUDITOR_ID))
                .andExpect(jsonPath("$.origenAsignacion").value("MANUAL"))
                .andExpect(jsonPath("$.fechaAsignacion").exists())
                .andExpect(jsonPath("$.estado").value("SOLICITUD_ENVIADA"));

        verify(solicitudAuditoriaService).asignarAuditor(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postDeAsignacionSobreSolicitudInexistenteDevuelve404() throws Exception {
        when(solicitudAuditoriaService.asignarAuditor(any(), any(), any()))
                .thenThrow(ApiException.solicitudAuditoriaNoEncontrada());

        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("manual"))
                        .principal(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Esta solicitud de auditoría no fue encontrada."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postDeAsignacionSobreSolicitudYaAsignadaAOtroAuditorDevuelve409() throws Exception {
        when(solicitudAuditoriaService.asignarAuditor(any(), any(), any()))
                .thenThrow(ApiException.asignacionAuditorPendiente());

        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("manual"))
                        .principal(principal()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Ya existe una solicitud de revisión pendiente con otro auditor."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postDeAsignacionConAuditorNoDisponibleDevuelve422() throws Exception {
        when(solicitudAuditoriaService.asignarAuditor(any(), any(), any()))
                .thenThrow(ApiException.auditorNoDisponible());

        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("manual"))
                        .principal(principal()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Este auditor no está disponible actualmente."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postDeAsignacionConOrigenFueraDelCatalogoDevuelve422() throws Exception {
        when(solicitudAuditoriaService.asignarAuditor(any(), any(), any()))
                .thenThrow(ApiException.origenAsignacionInvalido());

        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("sorteo"))
                        .principal(principal()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value("El origen de la asignación debe ser 'manual' o 'recomendacion_ia'."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postDeAsignacionConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/auditorias/{idSolicitud}/auditor", SOLICITUD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoAsignacion("manual"))
                        .principal(principal()))
                .andExpect(status().isForbidden());

        verify(solicitudAuditoriaService, never()).asignarAuditor(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postReporteValidoDevuelve200ConLaSolicitudActualizada() throws Exception {
        when(cargaReporteAuditoriaService.cargar(any(), any(), any(), any()))
                .thenReturn(detalleConReporte());

        mockMvc.perform(multipart("/api/auditorias/{idSolicitud}/reporte", SOLICITUD_ID)
                        .file(reportePdf())
                        .param("fechaAuditoriaRealizada", "2026-07-28")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REPORTE_CARGADO"))
                .andExpect(jsonPath("$.fechaAuditoriaRealizada").value("2026-07-28"))
                .andExpect(jsonPath("$.fechaCargaReporte").exists())
                .andExpect(jsonPath("$.reporteAuditoria.nombreArchivo").value("reporte.pdf"));

        verify(cargaReporteAuditoriaService).cargar(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postReporteConMimeInvalidoDevuelve422() throws Exception {
        when(cargaReporteAuditoriaService.cargar(any(), any(), any(), any()))
                .thenThrow(ApiException.reporteAuditoriaNoEsPdf());

        mockMvc.perform(multipart("/api/auditorias/{idSolicitud}/reporte", SOLICITUD_ID)
                        .file(reportePdf())
                        .param("fechaAuditoriaRealizada", "2026-07-28")
                        .principal(principal()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Solo se aceptan archivos en formato PDF."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postReporteDeAuditorIncorrectoDevuelve403() throws Exception {
        when(cargaReporteAuditoriaService.cargar(any(), any(), any(), any()))
                .thenThrow(ApiException.cargaReporteAuditoriaAjena());

        mockMvc.perform(multipart("/api/auditorias/{idSolicitud}/reporte", SOLICITUD_ID)
                        .file(reportePdf())
                        .param("fechaAuditoriaRealizada", "2026-07-28")
                        .principal(principal()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postReporteEnEstadoInvalidoDevuelve409() throws Exception {
        when(cargaReporteAuditoriaService.cargar(any(), any(), any(), any()))
                .thenThrow(ApiException.cargaReporteAuditoriaNoDisponible());

        mockMvc.perform(multipart("/api/auditorias/{idSolicitud}/reporte", SOLICITUD_ID)
                        .file(reportePdf())
                        .param("fechaAuditoriaRealizada", "2026-07-28")
                        .principal(principal()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("No es posible cargar el reporte en el estado actual de la solicitud."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postReporteConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(multipart("/api/auditorias/{idSolicitud}/reporte", SOLICITUD_ID)
                        .file(reportePdf())
                        .param("fechaAuditoriaRealizada", "2026-07-28")
                        .principal(principal()))
                .andExpect(status().isForbidden());

        verify(cargaReporteAuditoriaService, never()).cargar(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void getDevuelve200ConLaSolicitudSuAsignacionYElHistorial() throws Exception {
        when(solicitudAuditoriaDetalleService.obtenerDetalle(any(), any())).thenReturn(detalleAsignado());

        mockMvc.perform(get("/api/auditorias/{idSolicitud}", SOLICITUD_ID)
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAuditor").value(AUDITOR_ID))
                .andExpect(jsonPath("$.origenAsignacion").value("MANUAL"))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("SOLICITUD_ENVIADA"))
                .andExpect(jsonPath("$.historial[0].responsable").value("Marta Gerente"));

        verify(solicitudAuditoriaDetalleService).obtenerDetalle(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void getConRolDeAuditorDevuelve200PorqueElAuditorHaceSeguimiento() throws Exception {
        when(solicitudAuditoriaDetalleService.obtenerDetalle(any(), any())).thenReturn(detalleAsignado());

        mockMvc.perform(get("/api/auditorias/{idSolicitud}", SOLICITUD_ID)
                        .principal(principal()))
                .andExpect(status().isOk());

        verify(solicitudAuditoriaDetalleService).obtenerDetalle(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_PLATAFORMA")
    void getConRolDePlataformaDevuelve200() throws Exception {
        when(solicitudAuditoriaDetalleService.obtenerDetalle(any(), any())).thenReturn(detalleAsignado());

        mockMvc.perform(get("/api/auditorias/{idSolicitud}", SOLICITUD_ID)
                        .principal(principal()))
                .andExpect(status().isOk());

        verify(solicitudAuditoriaDetalleService).obtenerDetalle(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void getConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/auditorias/{idSolicitud}", SOLICITUD_ID)
                        .principal(principal()))
                .andExpect(status().isForbidden());

        verify(solicitudAuditoriaDetalleService, never()).obtenerDetalle(any(), any());
    }

    private static String cuerpoAsignacion(String origenAsignacion) {
        return """
                {"idAuditor":"%s","origenAsignacion":"%s"}"""
                .formatted(AUDITOR_ID, origenAsignacion);
    }

    private static SolicitudAuditoriaDetalleResponseDTO detalleAsignado() {
        SolicitudAuditoriaResponseDTO base = respuestaAsignada();
        SolicitudAuditoriaDetalleResponseDTO detalle = new SolicitudAuditoriaDetalleResponseDTO();
        detalle.setId(base.getId());
        detalle.setTipoCertificacion(base.getTipoCertificacion());
        detalle.setPeriodoInicio(base.getPeriodoInicio());
        detalle.setPeriodoFin(base.getPeriodoFin());
        detalle.setEstado(base.getEstado());
        detalle.setEstadoDescripcion(base.getEstado().getDescripcion());
        detalle.setFechaCreacion(base.getFechaCreacion());
        detalle.setDocumentos(base.getDocumentos());
        detalle.setIdAuditor(base.getIdAuditor());
        detalle.setAuditor(base.getAuditor());
        detalle.setOrigenAsignacion(base.getOrigenAsignacion());
        detalle.setFechaAsignacion(base.getFechaAsignacion());
        detalle.setNombreEmpresa("Acme S.A.");
        detalle.setHistorial(List.of(new TransicionEstadoAuditoriaResponseDTO(
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EventoTransicionAuditoria.SOLICITUD_CREADA,
                ActorTransicionAuditoria.EMPRESA,
                "Marta Gerente",
                Instant.parse("2026-07-27T18:00:00Z"))));
        return detalle;
    }

    private static SolicitudAuditoriaDetalleResponseDTO detalleConReporte() {
        SolicitudAuditoriaDetalleResponseDTO detalle = detalleAsignado();
        detalle.setEstado(EstadoSolicitudAuditoria.REPORTE_CARGADO);
        detalle.setFechaAuditoriaRealizada(LocalDate.of(2026, 7, 28));
        detalle.setFechaCargaReporte(Instant.parse("2026-07-28T18:00:00Z"));
        detalle.setReporteAuditoria(new com.piedpiper.carbonhub.auditoria.models.dtos.ReporteAuditoriaResponseDTO(
                UUID.randomUUID(), "reporte.pdf", 128L));
        return detalle;
    }

    private static SolicitudAuditoriaResponseDTO respuestaAsignada() {
        SolicitudAuditoriaResponseDTO respuesta = respuesta();
        respuesta.setIdAuditor(UUID.fromString(AUDITOR_ID));
        respuesta.setAuditor(new AuditorAsignadoResponseDTO(UUID.fromString(AUDITOR_ID), "Ana Auditora"));
        respuesta.setOrigenAsignacion(OrigenAsignacion.MANUAL);
        respuesta.setFechaAsignacion(Instant.parse("2026-07-27T19:00:00Z"));
        return respuesta;
    }

    private static MockMultipartFile datos() {
        String json = """
                {"periodoInicio":"2025-01-01",
                 "periodoFin":"2025-12-31",
                 "descripcionSolicitud":"Auditoría anual"}""";
        return new MockMultipartFile("datos", "datos.json",
                MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile documentoPdf() {
        return new MockMultipartFile("documentos", "respaldo.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 contenido de prueba".getBytes(StandardCharsets.US_ASCII));
    }

    private static MockMultipartFile reportePdf() {
        return new MockMultipartFile("reporteAuditoria", "reporte.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 contenido de prueba".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void getListadoDevuelve200ConLaPrimeraPagina() throws Exception {
        when(solicitudAuditoriaListadoService.listar(any(), any()))
                .thenReturn(new PaginaSolicitudesAuditoriaResponseDTO(List.of(), 0, 1, 0, 25));

        mockMvc.perform(get("/api/auditorias").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paginaActual").value(1))
                .andExpect(jsonPath("$.tamanioPagina").value(25))
                .andExpect(jsonPath("$.contenido").isArray());
    }

    /** Un estado inventado no rompe la peticion: el servicio lo descarta y devuelve todo. */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void getListadoConFiltroInvalidoDevuelve200SinError() throws Exception {
        when(solicitudAuditoriaListadoService.listar(any(), any()))
                .thenReturn(new PaginaSolicitudesAuditoriaResponseDTO(List.of(), 0, 1, 0, 25));

        mockMvc.perform(get("/api/auditorias")
                        .param("filtroEstado", "NO_EXISTE")
                        .principal(principal()))
                .andExpect(status().isOk());
    }

    /** Los parametros del filtro llegan al servicio ya convertidos, no como texto suelto. */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void losFiltrosDeLaQueryLleganAlServicio() throws Exception {
        when(solicitudAuditoriaListadoService.listar(any(), any()))
                .thenReturn(new PaginaSolicitudesAuditoriaResponseDTO(List.of(), 0, 2, 3, 25));

        mockMvc.perform(get("/api/auditorias")
                        .param("filtroEstado", "EN_REVISION", "REPORTE_CARGADO")
                        .param("pagina", "2")
                        .principal(principal()))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltrarSolicitudesAuditoriaRequestDTO> captor =
                ArgumentCaptor.forClass(FiltrarSolicitudesAuditoriaRequestDTO.class);
        verify(solicitudAuditoriaListadoService).listar(captor.capture(), any());
        assertThat(captor.getValue().getPagina()).isEqualTo(2);
        assertThat(captor.getValue().getFiltroEstado())
                .containsExactly("EN_REVISION", "REPORTE_CARGADO");
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void getListadoDeUnaEmpresaAjenaDevuelve403() throws Exception {
        when(solicitudAuditoriaListadoService.listar(any(), any()))
                .thenThrow(ApiException.listadoAuditoriasAjeno());

        mockMvc.perform(get("/api/auditorias")
                        .param("idEmpresa", "bbbbbbbb-2222-3333-4444-555566667777")
                        .principal(principal()))
                .andExpect(status().isForbidden());
    }

    /** El auditor tambien tiene listado: el endpoint dejo de ser solo de la empresa. */
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void unAuditorPuedeConsultarElListado() throws Exception {
        when(solicitudAuditoriaListadoService.listar(any(), any()))
                .thenReturn(new PaginaSolicitudesAuditoriaResponseDTO(List.of(), 0, 1, 0, 25));

        mockMvc.perform(get("/api/auditorias").principal(principal()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void unRolSinListadoDeAuditoriasRecibe403() throws Exception {
        mockMvc.perform(get("/api/auditorias").principal(principal()))
                .andExpect(status().isForbidden());

        verify(solicitudAuditoriaListadoService, never()).listar(any(), any());
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, null);
    }

    private static SolicitudAuditoriaResponseDTO respuesta() {
        return new SolicitudAuditoriaResponseDTO(
                UUID.randomUUID(),
                TipoCertificacionSolicitud.INICIAL,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                "Auditoría anual",
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                Instant.parse("2026-07-27T18:00:00Z"),
                List.of(new DocumentoRespaldoResponseDTO(UUID.randomUUID(), "respaldo.pdf", 27L)),
                null,
                null,
                null,
                null);
    }
}
