package com.piedpiper.carbonhub.empresa.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.service.ConfiguracionInicialEmpresaService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmpresaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "41ce47ab-a46c-4306-8c46-2688dc97fa73", roles = "ADMINISTRADOR_EMPRESA")
class EmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfiguracionInicialEmpresaService configuracionInicialEmpresaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String REQUEST_JSON = """
            {"nombreEmpresa":"Acme S.A.","cedulaJuridica":"3-101-123456","sectorIndustrial":"MANUFACTURA",
            "pais":"CR","cantidadEmpleados":50,"descripcion":"Empresa de prueba."}""";

    @Test
    void completarConfiguracionInicialValidoDevuelve201() throws Exception {
        when(configuracionInicialEmpresaService.completarPaso2(any(), any())).thenReturn(
                new ConfiguracionInicialEmpresaResponseDTO(UUID.randomUUID(), "Acme S.A.", "acme-s-a", true));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("acme-s-a"))
                .andExpect(jsonPath("$.documentosPendientes").value(true));
    }

    @Test
    void rolIncorrectoDevuelve403() throws Exception {
        when(configuracionInicialEmpresaService.completarPaso2(any(), any())).thenThrow(
                ApiException.accesoDenegado("Solo el administrador de una empresa puede completar este paso."));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void yaCompletadoDevuelve409() throws Exception {
        when(configuracionInicialEmpresaService.completarPaso2(any(), any())).thenThrow(
                ApiException.cuentaDuplicada("Ya completaste la configuración inicial de tu empresa."));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void cedulaJuridicaDuplicadaDevuelve409() throws Exception {
        when(configuracionInicialEmpresaService.completarPaso2(any(), any())).thenThrow(
                ApiException.cuentaDuplicada("Ya existe una empresa registrada con esta cédula jurídica."));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }
}
