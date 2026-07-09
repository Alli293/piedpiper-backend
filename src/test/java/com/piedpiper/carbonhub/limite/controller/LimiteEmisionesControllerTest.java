package com.piedpiper.carbonhub.limite.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesRequestDTO;
import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.service.LimiteEmisionesService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LimiteEmisionesController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LimiteEmisionesControllerTest {
    private static final Long EMPRESA_ID = 7L;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LimiteEmisionesService service;

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postValidoRetornaOkParaAdministradorEmpresa() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                "Meta anual"
        );
        when(service.guardarLimite(eq(EMPRESA_ID), any(LimiteEmisionesRequestDTO.class)))
                .thenReturn(new LimiteEmisionesResponseDTO(
                        1L,
                        EMPRESA_ID,
                        2026,
                        new BigDecimal("50.0000"),
                        "Meta anual",
                        "Limite vigente del anio 2026: 50.0000 t CO2e.",
                        null
                ));

        mockMvc.perform(post("/api/limites")
                        .header("X-Empresa-Id", EMPRESA_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postInvalidoRetornaBadRequest() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(2026, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/limites")
                        .header("X-Empresa-Id", EMPRESA_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USUARIO_GENERAL")
    void postConUsuarioGeneralEmpresaRetornaForbidden() throws Exception {
        LimiteEmisionesRequestDTO request = new LimiteEmisionesRequestDTO(
                2026,
                new BigDecimal("50.0000"),
                null
        );

        mockMvc.perform(post("/api/limites")
                        .header("X-Empresa-Id", EMPRESA_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void getListadoRetornaOkParaAdministradorEmpresa() throws Exception {
        when(service.listarLimites(EMPRESA_ID)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/limites")
                        .header("X-Empresa-Id", EMPRESA_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void deleteRetornaNoContentParaAdministradorEmpresa() throws Exception {
        mockMvc.perform(delete("/api/limites/2026")
                        .header("X-Empresa-Id", EMPRESA_ID))
                .andExpect(status().isNoContent());
    }
}
