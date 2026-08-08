package com.piedpiper.carbonhub.meta.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.meta.models.dtos.CrearMetaRequestDTO;
import com.piedpiper.carbonhub.meta.models.dtos.MetaResponseDTO;
import com.piedpiper.carbonhub.meta.service.MetaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

@WebMvcTest(controllers = MetaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtAuthenticationFilter.class
                }))
@AutoConfigureMockMvc(addFilters = false)
@Import(MetaControllerTest.MethodSecurityTestConfig.class)
class MetaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetaService metaService;

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }

    private MetaResponseDTO metaResponse() {
        return new MetaResponseDTO(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Reducir huella total",
                new BigDecimal("50.0000"),
                LocalDate.of(2027, 12, 31),
                new BigDecimal("30.0000"),
                60,
                false,
                Instant.parse("2026-08-06T00:00:00Z"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postValidoRetornaCreated() throws Exception {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Reducir huella total", new BigDecimal("50.0000"), LocalDate.of(2027, 12, 31));
        when(metaService.crear(eq(UUID.fromString(USUARIO_ID)), any(CrearMetaRequestDTO.class)))
                .thenReturn(metaResponse());

        mockMvc.perform(post("/api/metas")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreMeta").value("Reducir huella total"))
                .andExpect(jsonPath("$.progresoPorcentaje").value(60))
                .andExpect(jsonPath("$.vencida").value(false));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postConFechaLimitePasadaDevuelve422() throws Exception {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Meta invalida", new BigDecimal("50.0000"), LocalDate.of(2020, 1, 1));
        when(metaService.crear(eq(UUID.fromString(USUARIO_ID)), any(CrearMetaRequestDTO.class)))
                .thenThrow(ApiException.fechaLimiteMetaInvalida());

        mockMvc.perform(post("/api/metas")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La fecha límite debe ser una fecha futura."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postConNombreDemasiadoCortoDevuelve400() throws Exception {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "ab", new BigDecimal("50.0000"), LocalDate.of(2027, 12, 31));

        mockMvc.perform(post("/api/metas")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void postConFechaLimiteAusenteDevuelve400ConMensajeDistintoAlDeFechaPasada() throws Exception {
        String cuerpoSinFechaLimite = "{\"nombreMeta\":\"Reducir huella total\",\"valorObjetivoHuellaT\":50.0000}";

        mockMvc.perform(post("/api/metas")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(cuerpoSinFechaLimite))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fechaLimite: La fecha límite es obligatoria."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void postRolNoAutorizadoDevuelve403() throws Exception {
        CrearMetaRequestDTO request = new CrearMetaRequestDTO(
                "Reducir huella total", new BigDecimal("50.0000"), LocalDate.of(2027, 12, 31));

        mockMvc.perform(post("/api/metas")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void getDevuelve200ConElListado() throws Exception {
        when(metaService.listar(UUID.fromString(USUARIO_ID), "mes_actual", null))
                .thenReturn(List.of(metaResponse()));

        mockMvc.perform(get("/api/metas")
                        .principal(principal("ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("periodo", "mes_actual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreMeta").value("Reducir huella total"))
                .andExpect(jsonPath("$[0].progresoPorcentaje").value(60));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void getRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/metas")
                        .principal(principal("ROLE_AUDITOR_CERTIFICADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }
}
