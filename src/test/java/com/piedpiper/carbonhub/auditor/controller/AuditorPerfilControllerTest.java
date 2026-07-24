package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResultadoPerfil;
import com.piedpiper.carbonhub.auditor.service.AuditorPerfilService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditorPerfilController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AuditorPerfilControllerTest.MethodSecurityTestConfig.class)
class AuditorPerfilControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditorPerfilService auditorPerfilService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String AUDITOR_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final String BASE_URL = "/api/auditores/" + AUDITOR_ID + "/perfil";

    private static final String VALID_REQUEST_BODY = """
            {
                "especialidades": ["HUELLA_CARBONO", "ENERGIA_RENOVABLE"],
                "zonasCobertura": ["SAN_JOSE", "HEREDIA"],
                "disponible": true,
                "descripcionProfesional": "Auditor con experiencia en huella de carbono."
            }
            """;

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "AUDITOR_CERTIFICADO")
    void auditorCertificadoActualizaPerfilExistenteDevuelve200() throws Exception {
        PerfilAuditorResponseDTO responseDTO = new PerfilAuditorResponseDTO(
                UUID.fromString(AUDITOR_ID),
                List.of("HUELLA_CARBONO", "ENERGIA_RENOVABLE"),
                List.of("SAN_JOSE", "HEREDIA"),
                true,
                "Auditor con experiencia en huella de carbono.",
                Instant.now());

        when(auditorPerfilService.actualizar(any(), any(), any()))
                .thenReturn(new ResultadoPerfil(responseDTO, false));

        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_AUDITOR_CERTIFICADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditorId").value(AUDITOR_ID))
                .andExpect(jsonPath("$.especialidades[0]").value("HUELLA_CARBONO"))
                .andExpect(jsonPath("$.especialidades[1]").value("ENERGIA_RENOVABLE"))
                .andExpect(jsonPath("$.zonasCobertura[0]").value("SAN_JOSE"))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.descripcionProfesional").value("Auditor con experiencia en huella de carbono."));
    }

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "AUDITOR_CERTIFICADO")
    void auditorCertificadoCreaPerfilNuevoDevuelve201() throws Exception {
        PerfilAuditorResponseDTO responseDTO = new PerfilAuditorResponseDTO(
                UUID.fromString(AUDITOR_ID),
                List.of("HUELLA_CARBONO", "ENERGIA_RENOVABLE"),
                List.of("SAN_JOSE", "HEREDIA"),
                true,
                "Auditor con experiencia en huella de carbono.",
                Instant.now());

        when(auditorPerfilService.actualizar(any(), any(), any()))
                .thenReturn(new ResultadoPerfil(responseDTO, true));

        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_AUDITOR_CERTIFICADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auditorId").value(AUDITOR_ID))
                .andExpect(jsonPath("$.especialidades[0]").value("HUELLA_CARBONO"));
    }

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "USUARIO_GENERAL")
    void usuarioGeneralRecibe403() throws Exception {
        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_USUARIO_GENERAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacion_sliceSinFiltros_retorna500() throws Exception {
        // Sin SecurityContext, @PreAuthorize lanza AuthenticationCredentialsNotFoundException.
        // En producción el JwtAuthenticationFilter intercepta antes y retorna 401.
        // En este slice (addFilters=false + @EnableMethodSecurity), la excepción cae al
        // handler genérico → 500. El comportamiento real de rechazo queda validado.
        mockMvc.perform(put(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "AUDITOR_CERTIFICADO")
    void servicioLanzaPerfilNoPropioDevuelve403() throws Exception {
        when(auditorPerfilService.actualizar(any(), any(), any()))
                .thenThrow(ApiException.perfilNoPropio());

        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_AUDITOR_CERTIFICADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permiso para editar este perfil."));
    }

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "AUDITOR_CERTIFICADO")
    void especialidadesVaciasDevuelve400() throws Exception {
        String requestBody = """
                {
                    "especialidades": [],
                    "zonasCobertura": ["SAN_JOSE"],
                    "disponible": true,
                    "descripcionProfesional": null
                }
                """;

        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_AUDITOR_CERTIFICADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Seleccione al menos una especialidad.")));
    }

    @Test
    @WithMockUser(username = AUDITOR_ID, roles = "AUDITOR_CERTIFICADO")
    void descripcionExcede500CaracteresDevuelve400() throws Exception {
        String descripcionLarga = "A".repeat(501);
        String requestBody = """
                {
                    "especialidades": ["HUELLA_CARBONO"],
                    "zonasCobertura": ["SAN_JOSE"],
                    "disponible": true,
                    "descripcionProfesional": "%s"
                }
                """.formatted(descripcionLarga);

        mockMvc.perform(put(BASE_URL)
                        .principal(new TestingAuthenticationToken(AUDITOR_ID, null, "ROLE_AUDITOR_CERTIFICADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("La descripción no puede superar los 500 caracteres.")));
    }
}
