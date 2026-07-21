package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PaginaAuditoresResponseDTO;
import com.piedpiper.carbonhub.auditor.service.AuditorDirectorioService;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(AuditorControllerTest.MethodSecurityTestConfig.class)
class AuditorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditorDirectorioService auditorDirectorioService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static TestingAuthenticationToken principal(String rol) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", "ROLE_" + rol);
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void listarDevuelve200ConLaPaginaDeAuditores() throws Exception {
        AuditorResumenResponseDTO auditor = new AuditorResumenResponseDTO(
                UUID.randomUUID(), "Ana Mora", null, List.of("AGROINDUSTRIA"),
                new BigDecimal("4.5"), 30, true, 42, 8, "SAN_JOSE");
        when(auditorDirectorioService.listar(any(), anyInt(), any(), any()))
                .thenReturn(new PaginaAuditoresResponseDTO(List.of(auditor), 1, 0, 1));

        mockMvc.perform(get("/api/auditores").principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Ana Mora"))
                .andExpect(jsonPath("$.contenido[0].auditoriasCompletadas").value(42))
                .andExpect(jsonPath("$.totalResultados").value(1))
                .andExpect(jsonPath("$.paginaActual").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void auditorCertificadoTambienPuedeConsultarElDirectorio() throws Exception {
        when(auditorDirectorioService.listar(any(), anyInt(), any(), any()))
                .thenReturn(new PaginaAuditoresResponseDTO(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/auditores").principal(principal("AUDITOR_CERTIFICADO")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void ordenamientoInvalidoDevuelve400() throws Exception {
        when(auditorDirectorioService.listar(any(), anyInt(), any(), any()))
                .thenThrow(ApiException.ordenamientoAuditoresInvalido());

        mockMvc.perform(get("/api/auditores").param("ordenamiento", "POR_PRECIO")
                        .principal(principal("ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_PLATAFORMA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/auditores").principal(principal("ADMINISTRADOR_PLATAFORMA")))
                .andExpect(status().isForbidden());
    }
}
