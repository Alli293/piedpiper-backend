package com.piedpiper.carbonhub.limite.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;
import com.piedpiper.carbonhub.limite.config.LimiteSecurityConfig;
import com.piedpiper.carbonhub.limite.service.EmpresaAutenticadaService;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.service.LimiteEmisionesService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LimiteEmisionesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        LimiteSecurityConfig.class
})
class LimiteEmisionesControllerTest {
    private static final UUID EMPRESA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String USUARIO_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private EmpresaAutenticadaService empresaAutenticadaService;

    @MockitoBean
    private LimiteEmisionesService service;

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postValidoRetornaCreatedCuandoCreaLimite() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(empresaAutenticadaService.obtenerEmpresaId(any())).thenReturn(EMPRESA_ID);
        when(service.guardarLimite(eq(EMPRESA_ID), any(LimiteEmisionesRequestDTO.class)))
                .thenReturn(new LimiteEmisionesResponseDTO(
                        1L,
                        EMPRESA_ID,
                        2026,
                        new BigDecimal("50.0000"),
                        "Meta anual",
                        "Limite vigente del anio 2026: 50.0000 t CO2e.",
                        null,
                        true
                ));

        mockMvc.perform(post("/api/limites")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postValidoRetornaOkCuandoActualizaLimite() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(empresaAutenticadaService.obtenerEmpresaId(any())).thenReturn(EMPRESA_ID);
        when(service.guardarLimite(eq(EMPRESA_ID), any(LimiteEmisionesRequestDTO.class)))
                .thenReturn(new LimiteEmisionesResponseDTO(
                        1L,
                        EMPRESA_ID,
                        2026,
                        new BigDecimal("50.0000"),
                        "Meta anual",
                        "Limite vigente del anio 2026: 50.0000 t CO2e.",
                        null,
                        false
                ));

        mockMvc.perform(post("/api/limites")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postInvalidoRetornaBadRequest() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(2026, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/limites")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_GENERAL")
    void postConUsuarioGeneralEmpresaRetornaForbidden() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                null
        );

        mockMvc.perform(post("/api/limites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void getListadoRetornaOkParaAdministradorEmpresa() throws Exception {
        when(empresaAutenticadaService.obtenerEmpresaId(any())).thenReturn(EMPRESA_ID);
        when(service.listarLimites(EMPRESA_ID)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/limites")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void deleteRetornaNoContentParaAdministradorEmpresa() throws Exception {
        when(empresaAutenticadaService.obtenerEmpresaId(any())).thenReturn(EMPRESA_ID);

        mockMvc.perform(delete("/api/limites/2026")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNoContent());
    }

    private TestingAuthenticationToken authentication(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }
}
