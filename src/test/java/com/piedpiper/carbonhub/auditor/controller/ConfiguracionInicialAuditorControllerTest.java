package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.service.ConfiguracionInicialAuditorService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.models.dtos.MensajeResponseDTO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConfiguracionInicialAuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(ConfiguracionInicialAuditorControllerTest.MethodSecurityTestConfig.class)
class ConfiguracionInicialAuditorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfiguracionInicialAuditorService configuracionInicialAuditorService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postValidoDevuelve201ConElMensaje() throws Exception {
        when(configuracionInicialAuditorService.completar(any(), any(), any()))
                .thenReturn(new MensajeResponseDTO("Recibimos tu información."));

        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Recibimos tu información."));

        verify(configuracionInicialAuditorService).completar(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isForbidden());

        verify(configuracionInicialAuditorService, never()).completar(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void sinLaParteDocumentosDevuelve400() throws Exception {
        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datos())
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void sinEspecialidadesDevuelve400() throws Exception {
        MockMultipartFile datosSinEspecialidades = new MockMultipartFile("datos", "datos.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"aniosExperiencia\":5,\"especialidades\":[]}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datosSinEspecialidades)
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void configuracionNoDisponibleDevuelve409() throws Exception {
        when(configuracionInicialAuditorService.completar(any(), any(), any()))
                .thenThrow(ApiException.configuracionAuditorNoDisponible());

        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void especialidadesInvalidasDevuelve400() throws Exception {
        when(configuracionInicialAuditorService.completar(any(), any(), any()))
                .thenThrow(ApiException.especialidadesInvalidas(java.util.List.of("BUCEO")));

        mockMvc.perform(multipart("/api/auditor/configuracion-inicial")
                        .file(datos())
                        .file(documentoPdf())
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    private static MockMultipartFile datos() {
        String json = """
                {"aniosExperiencia":5,
                 "especialidades":["MANUFACTURA"],
                 "descripcionProfesional":"Experiencia en manufactura sostenible."}""";
        return new MockMultipartFile("datos", "datos.json",
                MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile documentoPdf() {
        return new MockMultipartFile("documentos", "cert.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7 contenido de prueba".getBytes(StandardCharsets.US_ASCII));
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, null);
    }
}
