package com.piedpiper.carbonhub.auditoria.controller;

import com.piedpiper.carbonhub.auditoria.service.DecisionAuditorService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DecisionAuditorController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(DecisionAuditorControllerTest.MethodSecurityTestConfig.class)
class DecisionAuditorControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String USUARIO_ID = "c0ffee00-1111-2222-3333-444455556666";
    private static final String SOLICITUD_ID = "9a1c0a6e-58b2-4d18-9d3e-3a4b5c6d7e8f";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecisionAuditorService decisionAuditorService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void aceptacionValidaDevuelve200() throws Exception {
        mockMvc.perform(peticion("""
                        {"decision":"aceptada"}"""))
                .andExpect(status().isOk());

        verify(decisionAuditorService).responder(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void rechazoConMotivoValidoDevuelve200() throws Exception {
        mockMvc.perform(peticion("""
                        {"decision":"rechazada","motivoRechazo":"No tengo disponibilidad este mes"}"""))
                .andExpect(status().isOk());

        verify(decisionAuditorService).responder(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void motivoDeRechazoMuyCortoDevuelve400SinLlegarAlServicio() throws Exception {
        mockMvc.perform(peticion("""
                        {"decision":"rechazada","motivoRechazo":"corto"}"""))
                .andExpect(status().isBadRequest());

        verify(decisionAuditorService, never()).responder(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void decisionVaciaDevuelve400() throws Exception {
        mockMvc.perform(peticion("""
                        {"decision":""}"""))
                .andExpect(status().isBadRequest());

        verify(decisionAuditorService, never()).responder(any(), any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void decisionInvalidaDevuelve422() throws Exception {
        doThrow(ApiException.decisionAuditorInvalida())
                .when(decisionAuditorService).responder(any(), any(), any());

        mockMvc.perform(peticion("""
                        {"decision":"quizas"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La decisión debe ser 'aceptada' o 'rechazada'."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void fueraDePlazoDevuelve409() throws Exception {
        doThrow(ApiException.decisionAuditorNoDisponible())
                .when(decisionAuditorService).responder(any(), any(), any());

        mockMvc.perform(peticion("""
                        {"decision":"aceptada"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Esta solicitud ya no está disponible para tu respuesta."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "AUDITOR_CERTIFICADO")
    void auditorDistintoDelAsignadoDevuelve403() throws Exception {
        doThrow(ApiException.decisionAuditorAjena())
                .when(decisionAuditorService).responder(any(), any(), any());

        mockMvc.perform(peticion("""
                        {"decision":"aceptada"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "ADMINISTRADOR_EMPRESA")
    void laEmpresaNoPuedeResponderPorElAuditor() throws Exception {
        mockMvc.perform(peticion("""
                        {"decision":"aceptada"}"""))
                .andExpect(status().isForbidden());

        verify(decisionAuditorService, never()).responder(any(), any(), any());
    }

    private static org.springframework.test.web.servlet.RequestBuilder peticion(String cuerpo) {
        return post("/api/auditorias/{idSolicitud}/decision", SOLICITUD_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .principal(principal());
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, "n/a", List.of());
    }
}
