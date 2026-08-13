package com.piedpiper.carbonhub.validacion.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import com.piedpiper.carbonhub.validacion.models.dtos.MiSolicitudAuditorResponseDTO;
import com.piedpiper.carbonhub.validacion.service.MiSolicitudAuditorService;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MiSolicitudAuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(MiSolicitudAuditorControllerTest.MethodSecurityTestConfig.class)
class MiSolicitudAuditorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MiSolicitudAuditorService miSolicitudAuditorService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void devuelve200ConElEstadoDeLaSolicitud() throws Exception {
        when(miSolicitudAuditorService.obtener(any())).thenReturn(
                new MiSolicitudAuditorResponseDTO("PENDIENTE", Instant.parse("2026-08-01T12:00:00Z"), null, null));

        mockMvc.perform(get("/api/auditor/mi-solicitud").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fechaResolucion").doesNotExist());
    }

    // El auditor RECHAZADO solo recibe ROLE_AUDITOR_RECHAZADO (ver
    // JwtAuthenticationFilter.autoridadesPara), y este es el unico endpoint de auditor al que ese
    // rol da acceso: le permite ver el motivo de su rechazo sin destrabar el resto del rol operativo.
    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_RECHAZADO")
    void auditorRechazadoPuedeConsultarSuPropiaSolicitud() throws Exception {
        when(miSolicitudAuditorService.obtener(any())).thenReturn(
                new MiSolicitudAuditorResponseDTO("RECHAZADA", Instant.parse("2026-08-01T12:00:00Z"),
                        Instant.parse("2026-08-05T09:00:00Z"), "Documentos ilegibles"));

        mockMvc.perform(get("/api/auditor/mi-solicitud").principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_PLATAFORMA")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/auditor/mi-solicitud").principal(principal()))
                .andExpect(status().isForbidden());

        verify(miSolicitudAuditorService, never()).obtener(any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void sinSolicitudPropiaDevuelve404() throws Exception {
        when(miSolicitudAuditorService.obtener(any()))
                .thenThrow(ApiException.solicitudValidacionNoEncontrada());

        mockMvc.perform(get("/api/auditor/mi-solicitud").principal(principal()))
                .andExpect(status().isNotFound());
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, null);
    }
}
