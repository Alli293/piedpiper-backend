package com.piedpiper.carbonhub.validacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.dtos.DocumentoCredencialResumenResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.PaginaSolicitudesResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudDetalleResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudPendienteResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.DocumentoCredencialAuditor;
import com.piedpiper.carbonhub.validacion.service.ValidacionAuditorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ValidacionAuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class ValidacionAuditorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidacionAuditorService validacionAuditorService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication AUTHENTICATION = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_PLATAFORMA")));

    @Test
    void listarPendientesDevuelve200ConLaPagina() throws Exception {
        when(validacionAuditorService.listarPendientes(any(), anyInt())).thenReturn(
                new PaginaSolicitudesResponseDTO(
                        List.of(new SolicitudPendienteResponseDTO(
                                UUID.randomUUID(), "Ana Mora", "ana@correo.com", Instant.now())),
                        0, 1, 1));

        mockMvc.perform(get("/api/admin/solicitudes-auditor").principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombreAuditor").value("Ana Mora"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void resolverAprobadoDevuelve200() throws Exception {
        when(validacionAuditorService.resolver(any(), any(), any())).thenReturn(
                new SolicitudResueltaResponseDTO(
                        UUID.randomUUID(), "APROBADO", "ACTIVO", Instant.now(), null));

        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"aprobado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.estadoAuditor").value("ACTIVO"));
    }

    @Test
    void rolDistintoDevuelve403() throws Exception {
        when(validacionAuditorService.resolver(any(), any(), any())).thenThrow(
                ApiException.accesoDenegado("Solo el administrador de la plataforma puede gestionar solicitudes."));

        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"aprobado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void decisionInvalidaDevuelve422() throws Exception {
        when(validacionAuditorService.resolver(any(), any(), any())).thenThrow(
                ApiException.valorNoSoportado("La decisión debe ser 'aprobado' o 'rechazado'."));

        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"otro\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void solicitudInexistenteDevuelve404() throws Exception {
        when(validacionAuditorService.resolver(any(), any(), any()))
                .thenThrow(ApiException.solicitudNoEncontrada());

        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"aprobado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void solicitudYaProcesadaDevuelve409() throws Exception {
        when(validacionAuditorService.resolver(any(), any(), any()))
                .thenThrow(ApiException.solicitudYaProcesada());

        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"aprobado\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void sinDecisionDevuelve400() throws Exception {
        mockMvc.perform(post("/api/admin/solicitudes-auditor/" + UUID.randomUUID() + "/decision")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerDetalleDevuelve200ConElPerfilYLosDocumentos() throws Exception {
        UUID solicitudId = UUID.randomUUID();
        when(validacionAuditorService.obtenerDetalle(any(), any())).thenReturn(new SolicitudDetalleResponseDTO(
                solicitudId, "Ana Mora", "ana@correo.com", "PENDIENTE", Instant.now(), 8,
                List.of("AGROINDUSTRIA", "MANUFACTURA"), "Descripción profesional", null,
                List.of(new DocumentoCredencialResumenResponseDTO(UUID.randomUUID(), "cert.pdf", 1024L))));

        mockMvc.perform(get("/api/admin/solicitudes-auditor/" + solicitudId).principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAuditor").value("Ana Mora"))
                .andExpect(jsonPath("$.especialidades[0]").value("AGROINDUSTRIA"))
                .andExpect(jsonPath("$.documentos[0].nombreArchivo").value("cert.pdf"))
                .andExpect(jsonPath("$.documentos[0].contenido").doesNotExist());
    }

    @Test
    void obtenerDetalleDeSolicitudInexistenteDevuelve404() throws Exception {
        when(validacionAuditorService.obtenerDetalle(any(), any()))
                .thenThrow(ApiException.solicitudNoEncontrada());

        mockMvc.perform(get("/api/admin/solicitudes-auditor/" + UUID.randomUUID()).principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }

    @Test
    void descargarDocumentoDevuelveElPdfComoAdjunto() throws Exception {
        when(validacionAuditorService.obtenerDocumento(any(), any(), any())).thenReturn(
                DocumentoCredencialAuditor.builder()
                        .nombreArchivo("cert.pdf")
                        .tipoContenido("application/pdf")
                        .contenido("%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8))
                        .build());

        mockMvc.perform(get("/api/admin/solicitudes-auditor/" + UUID.randomUUID()
                        + "/documentos/" + UUID.randomUUID())
                        .principal(AUTHENTICATION))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("cert.pdf")));
    }

    @Test
    void descargarDocumentoDeOtraSolicitudDevuelve404() throws Exception {
        when(validacionAuditorService.obtenerDocumento(any(), any(), any()))
                .thenThrow(ApiException.documentoCredencialNoEncontrado());

        mockMvc.perform(get("/api/admin/solicitudes-auditor/" + UUID.randomUUID()
                        + "/documentos/" + UUID.randomUUID())
                        .principal(AUTHENTICATION))
                .andExpect(status().isNotFound());
    }
}
