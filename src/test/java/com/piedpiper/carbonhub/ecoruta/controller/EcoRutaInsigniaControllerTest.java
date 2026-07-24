package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.ecoruta.models.dtos.InsigniaUsuarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaInsigniaService;
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

@WebMvcTest(controllers = EcoRutaInsigniaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EcoRutaInsigniaControllerTest.MethodSecurityTestConfig.class)
class EcoRutaInsigniaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final UUID USUARIO_ID =
            UUID.fromString("41ce47ab-a46c-4306-8c46-2688dc97fa73");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EcoRutaInsigniaService ecoRutaInsigniaService;

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73",
            roles = "USUARIO_INDIVIDUAL")
    void usuarioIndividualListaSusInsigniasObtenidas() throws Exception {
        when(ecoRutaInsigniaService.listarObtenidas(USUARIO_ID)).thenReturn(List.of(
                new InsigniaUsuarioResponseDTO(2L, "EcoScore Excelente",
                        "Completaste tu primer itinerario sostenible.",
                        "primer_itinerario_sostenible",
                        Instant.parse("2026-07-15T20:32:00Z"))));

        mockMvc.perform(get("/api/ecoruta/insignias/me")
                        .principal(principal("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idInsignia").value(2))
                .andExpect(jsonPath("$[0].nombre").value("EcoScore Excelente"))
                .andExpect(jsonPath("$[0].eventoDesbloqueo")
                        .value("primer_itinerario_sostenible"));

        verify(ecoRutaInsigniaService).listarObtenidas(USUARIO_ID);
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73",
            roles = "ADMINISTRADOR_EMPRESA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/ecoruta/insignias/me")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isForbidden());
    }

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID.toString(), "password", authority);
    }
}
