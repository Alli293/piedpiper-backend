package com.piedpiper.carbonhub.ecoruta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.PreferenciasViajeService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EcoRutaPreferenciasController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CorsConfig.class
})
class EcoRutaPreferenciasControllerTest {

    private static final String USUARIO_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private PreferenciasViajeService service;

    private PreferenciasViajeRequestDTO requestValido() {
        return new PreferenciasViajeRequestDTO(
                5,
                LocalDate.now().plusDays(10),
                "FAMILIA",
                "MODERADO",
                List.of("NATURALEZA", "AVENTURA"),
                "SAN_JOSE",
                "San José, Costa Rica",
                true,
                null,
                false
        );
    }

    private PreferenciasViajeResponseDTO respuesta(boolean recienCreada) {
        PreferenciasViajeResponseDTO response = new PreferenciasViajeResponseDTO();
        response.setId(UUID.randomUUID());
        response.setCantidadDias(5);
        response.setFechaInicio(LocalDate.now().plusDays(10));
        response.setTipoViaje("FAMILIA");
        response.setIntereses(List.of("NATURALEZA", "AVENTURA"));
        response.setProvinciaPreferida("SAN_JOSE");
        response.setUbicacionActual("San José, Costa Rica");
        response.setBuscarCercaDeMi(true);
        response.setConversacionCompleta(true);
        response.setRecienCreada(recienCreada);
        return response;
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postValidoRetornaCreatedCuandoEsElPrimerGuardado() throws Exception {
        when(service.guardar(any(UUID.class), any(PreferenciasViajeRequestDTO.class)))
                .thenReturn(respuesta(true));

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postValidoRetornaOkCuandoActualizaUnRegistroExistente() throws Exception {
        when(service.guardar(any(UUID.class), any(PreferenciasViajeRequestDTO.class)))
                .thenReturn(respuesta(false));

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postConCantidadDeDiasFueraDeRangoDevuelve400() throws Exception {
        PreferenciasViajeRequestDTO request = requestValido();
        request.setCantidadDias(31);

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postSinInteresesDevuelve400() throws Exception {
        PreferenciasViajeRequestDTO request = requestValido();
        request.setIntereses(List.of());

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postConBuscarCercaDeMiSinUbicacionDevuelve400() throws Exception {
        PreferenciasViajeRequestDTO request = requestValido();
        request.setBuscarCercaDeMi(true);
        request.setUbicacionActual(null);

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postConPresupuestoDemasiadoLargoDevuelve400() throws Exception {
        PreferenciasViajeRequestDTO request = requestValido();
        request.setPresupuesto("x".repeat(101));

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postConUbicacionActualDemasiadoLargaDevuelve400() throws Exception {
        PreferenciasViajeRequestDTO request = requestValido();
        request.setUbicacionActual("x".repeat(201));

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postConConflictoDeGuardadoDevuelve409() throws Exception {
        when(service.guardar(any(UUID.class), any(PreferenciasViajeRequestDTO.class)))
                .thenThrow(ApiException.preferenciasViajeConflicto());

        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void getSinRegistroPrevioDevuelve404() throws Exception {
        when(service.obtener(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void getConRegistroPrevioDevuelveOk() throws Exception {
        when(service.obtener(any(UUID.class))).thenReturn(Optional.of(respuesta(false)));

        mockMvc.perform(get("/api/ecoruta/preferencias")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoViaje").value("FAMILIA"));
    }

    private TestingAuthenticationToken authentication(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }
}
