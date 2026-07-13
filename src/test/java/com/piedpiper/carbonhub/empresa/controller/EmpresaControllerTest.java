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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Authentication AUTHENTICATION = new UsernamePasswordAuthenticationToken(
            USUARIO_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR_EMPRESA")));

    @Test
    void completarConfiguracionInicialValidoDevuelve201() throws Exception {
        when(configuracionInicialEmpresaService.completarConfiguracionEmpresa(any(), any())).thenReturn(
                new ConfiguracionInicialEmpresaResponseDTO(
                        UUID.randomUUID(), "Acme S.A.", "acme-s-a", true, true));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("acme-s-a"))
                .andExpect(jsonPath("$.documentosPendientes").value(true));
    }

    @Test
    void rolIncorrectoDevuelve403() throws Exception {
        when(configuracionInicialEmpresaService.completarConfiguracionEmpresa(any(), any())).thenThrow(
                ApiException.accesoDenegado("Solo el administrador de una empresa puede completar este paso."));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void yaCompletadoDevuelve200ConDatosDeLaEmpresaExistente() throws Exception {
        when(configuracionInicialEmpresaService.completarConfiguracionEmpresa(any(), any())).thenReturn(
                new ConfiguracionInicialEmpresaResponseDTO(
                        UUID.randomUUID(), "Acme Existente S.A.", "acme-existente-s-a", true, false));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreEmpresa").value("Acme Existente S.A."))
                .andExpect(jsonPath("$.slug").value("acme-existente-s-a"));
    }

    @Test
    void cedulaJuridicaDuplicadaDevuelve409() throws Exception {
        when(configuracionInicialEmpresaService.completarConfiguracionEmpresa(any(), any())).thenThrow(
                ApiException.cuentaDuplicada("Ya existe una empresa registrada con esta cédula jurídica."));

        mockMvc.perform(post("/api/empresas/configuracion-inicial")
                        .principal(AUTHENTICATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }
}
