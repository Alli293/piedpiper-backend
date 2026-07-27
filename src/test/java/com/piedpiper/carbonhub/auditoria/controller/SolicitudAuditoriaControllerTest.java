package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.service.SolicitudAuditoriaService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudAuditoriaService solicitudAuditoriaService;
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
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(multipart("/api/auditorias")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isForbidden());
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
                List.of(new DocumentoRespaldoResponseDTO(UUID.randomUUID(), "respaldo.pdf", 27L)));
    }
}
