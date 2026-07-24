package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaInsigniaService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EcoRutaCertificacionEventoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EcoRutaCertificacionEventoControllerTest.MethodSecurityTestConfig.class)
class EcoRutaCertificacionEventoControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String REQUEST_VALIDO = """
            {
              "usuario_id": "41ce47ab-a46c-4306-8c46-2688dc97fa73",
              "evento_generado": "primer_itinerario_sostenible",
              "fecha_evento": "2026-07-15T20:32:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EcoRutaInsigniaService ecoRutaInsigniaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = "certificacion", roles = "CERTIFICACION")
    void eventoValidoInvocaServicioYDevuelve200() throws Exception {
        mockMvc.perform(post("/api/certificacion/eventos")
                        .principal(principal("ROLE_CERTIFICACION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isOk());

        verify(ecoRutaInsigniaService).evaluarYOtorgar(any(EventoCertificacionRequestDTO.class));
    }

    @Test
    @WithMockUser(username = "usuario", roles = "USUARIO_INDIVIDUAL")
    void rolNoAutorizadoDevuelve403YNoInvocaServicio() throws Exception {
        mockMvc.perform(post("/api/certificacion/eventos")
                        .principal(principal("ROLE_USUARIO_INDIVIDUAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ecoRutaInsigniaService);
    }

    @Test
    @WithMockUser(username = "certificacion", roles = "CERTIFICACION")
    void excepcionInesperadaDelServicioDevuelve500() throws Exception {
        doThrow(new RuntimeException("fallo silencioso")).when(ecoRutaInsigniaService)
                .evaluarYOtorgar(any());

        mockMvc.perform(post("/api/certificacion/eventos")
                        .principal(principal("ROLE_CERTIFICACION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isInternalServerError());
    }

    private TestingAuthenticationToken principal(String authority) {
        return new TestingAuthenticationToken("certificacion", "password", authority);
    }
}
