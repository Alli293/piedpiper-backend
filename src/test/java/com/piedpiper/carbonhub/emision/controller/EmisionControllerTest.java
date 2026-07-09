package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;
import com.piedpiper.carbonhub.emision.service.EmisionElectricidadService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmisionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EmisionControllerTest.MethodSecurityTestConfig.class)
class EmisionControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisionElectricidadService emisionElectricidadService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String REQUEST_VALIDO = "{\"titulo\":\"Consumo oficina central\","
            + "\"electricityValue\":500,\"electricityUnit\":\"kwh\",\"fechaActividad\":\"2026-07-01\"}";

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void registroValidoComoAdministradorEmpresaDevuelve201() throws Exception {
        EmisionResponseDTO response = new EmisionResponseDTO(UUID.randomUUID(), CategoriaEmision.ELECTRICIDAD,
                "Consumo oficina central", LocalDate.now(), new BigDecimal("500"), UnidadElectricidad.KWH,
                new BigDecimal("237.5"), new BigDecimal("0.2375"), "ci-estimate-id", Instant.now(), Instant.now());
        when(emisionElectricidadService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "USUARIO_GENERAL")
    void registroValidoComoUsuarioGeneralDevuelve201() throws Exception {
        EmisionResponseDTO response = new EmisionResponseDTO(UUID.randomUUID(), CategoriaEmision.ELECTRICIDAD,
                "Consumo oficina central", LocalDate.now(), new BigDecimal("500"), UnidadElectricidad.KWH,
                new BigDecimal("237.5"), new BigDecimal("0.2375"), "ci-estimate-id", Instant.now(), Instant.now());
        when(emisionElectricidadService.registrar(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void cuerpoInvalidoDevuelve400() throws Exception {
        String invalido = "{\"titulo\":\"Consumo\",\"electricityValue\":-5,\"electricityUnit\":\"kwh\","
                + "\"fechaActividad\":\"2026-07-01\"}";

        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/emisiones/electricidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isForbidden());
    }
}
