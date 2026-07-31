package com.piedpiper.carbonhub.emision.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.dtos.EmisionVueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import com.piedpiper.carbonhub.emision.service.EmisionConsultaService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmisionConsultaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EmisionConsultaControllerTest.MethodSecurityTestConfig.class)
class EmisionConsultaControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmisionConsultaService emisionConsultaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String ADMIN_USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    private TestingAuthenticationToken principal(String usuarioId, String authority) {
        return new TestingAuthenticationToken(usuarioId, "password", authority);
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarEmisionesDevuelve200() throws Exception {
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setId(UUID.randomUUID());
        response.setCategoria(CategoriaEmision.VUELO);
        response.setTitulo("Viaje aereo SFO-YYZ");
        response.setCarbonKg(new BigDecimal("237.5"));
        when(emisionConsultaService.listar(any(), isNull(), isNull(), isNull())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/emisiones")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("VUELO"))
                .andExpect(jsonPath("$[0].carbonKg").value(237.5));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarEmisionesConFiltrosDevuelve200() throws Exception {
        EmisionResponseDTO response = new EmisionFlotaResponseDTO();
        response.setId(UUID.randomUUID());
        response.setCategoria(CategoriaEmision.FLOTA);
        response.setTitulo("Recorrido Toyota Corolla");
        response.setCarbonKg(new BigDecimal("18.9"));
        when(emisionConsultaService.listar(any(), eq(CategoriaEmision.FLOTA), eq(2026), eq(7)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/emisiones")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("categoria", "FLOTA")
                        .param("anio", "2026")
                        .param("mes", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoria").value("FLOTA"));

        verify(emisionConsultaService).listar(any(), eq(CategoriaEmision.FLOTA), eq(2026), eq(7));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarEmisionesCategoriaTodasNoFiltraPorCategoria() throws Exception {
        when(emisionConsultaService.listar(any(), isNull(), eq(2026), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/emisiones")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("categoria", "TODAS")
                        .param("anio", "2026"))
                .andExpect(status().isOk());

        verify(emisionConsultaService).listar(any(), isNull(), eq(2026), isNull());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void listarEmisionesCategoriaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(get("/api/emisiones")
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA"))
                        .param("categoria", "OTRA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void obtenerEmisionDevuelve200() throws Exception {
        UUID id = UUID.randomUUID();
        EmisionResponseDTO response = new EmisionVueloResponseDTO();
        response.setId(id);
        response.setCategoria(CategoriaEmision.VUELO);
        response.setTitulo("Viaje aereo SFO-YYZ");
        when(emisionConsultaService.obtener(eq(id), any())).thenReturn(response);

        mockMvc.perform(get("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.categoria").value("VUELO"));
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void obtenerEmisionNoEncontradaDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(emisionConsultaService.obtener(eq(id), any()))
                .thenThrow(ApiException.recursoNoEncontrado("No se encontro la emision solicitada."));

        mockMvc.perform(get("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void eliminarEmisionDevuelve204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNoContent());

        verify(emisionConsultaService).eliminar(eq(id), any());
    }

    @Test
    @WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
    void eliminarEmisionNoEncontradaDevuelve404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(ApiException.recursoNoEncontrado("No se encontro la emision solicitada."))
                .when(emisionConsultaService).eliminar(eq(id), any());

        mockMvc.perform(delete("/api/emisiones/{id}", id)
                        .principal(principal(ADMIN_USUARIO_ID, "ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "db2ed1e7-6719-4595-844e-68efffe146cf", roles = "AUDITOR_CERTIFICADO")
    void endpointsConRolNoAutorizadoDevuelven403() throws Exception {
        UUID id = UUID.randomUUID();
        String mensajeEsperado = "No tiene permisos para realizar esta acción.";

        mockMvc.perform(get("/api/emisiones"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
        mockMvc.perform(get("/api/emisiones/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
        mockMvc.perform(delete("/api/emisiones/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(mensajeEsperado));
    }
}
