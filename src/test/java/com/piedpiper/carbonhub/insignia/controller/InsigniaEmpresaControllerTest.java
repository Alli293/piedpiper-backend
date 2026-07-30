package com.piedpiper.carbonhub.insignia.controller;

import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;

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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InsigniaEmpresaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                }))
@AutoConfigureMockMvc(addFilters = false)
@Import(InsigniaEmpresaControllerTest.MethodSecurityTestConfig.class)
class InsigniaEmpresaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsigniaEmpresaConsultaService insigniaEmpresaConsultaService;

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void listarRetornaLasInsigniasDeLaEmpresaAutenticada() throws Exception {
        when(insigniaEmpresaConsultaService.listarParaEmpresaAutenticada(UUID.fromString(USUARIO_ID)))
                .thenReturn(List.of(new InsigniaEmpresaResponseDTO(
                        1L,
                        "bronce",
                        "Carbono Neutral",
                        "Primera insignia empresarial.",
                        Instant.parse("2026-01-15T00:00:00Z")
                )));

        mockMvc.perform(get("/api/empresas/insignias")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idInsignia").value(1))
                .andExpect(jsonPath("$[0].nivelInsignia").value("bronce"))
                .andExpect(jsonPath("$[0].nombre").value("Carbono Neutral"))
                .andExpect(jsonPath("$[0].fechaObtencion").value("2026-01-15T00:00:00Z"));

        verify(insigniaEmpresaConsultaService).listarParaEmpresaAutenticada(
                UUID.fromString(USUARIO_ID));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void usuarioGeneralDeEmpresaPuedeListarInsignias() throws Exception {
        when(insigniaEmpresaConsultaService.listarParaEmpresaAutenticada(UUID.fromString(USUARIO_ID)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/empresas/insignias")
                        .principal(principal("ROLE_USUARIO_GENERAL")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/empresas/insignias")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }
}
