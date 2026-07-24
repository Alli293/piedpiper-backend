package com.piedpiper.carbonhub.reconocimiento.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoReconocimientoResponseDTO;
import com.piedpiper.carbonhub.reconocimiento.models.enums.EstadoEnvioCertificacion;
import com.piedpiper.carbonhub.reconocimiento.service.EventoReconocimientoService;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventoReconocimientoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(EventoReconocimientoControllerTest.MethodSecurityTestConfig.class)
class EventoReconocimientoControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoReconocimientoService eventoReconocimientoService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private static final String USUARIO_ID = "41ce47ab-a46c-4306-8c46-2688dc97fa73";
    private static final Instant FECHA_EVENTO = Instant.parse("2026-07-22T18:00:00Z");

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void postEventoValidoDevuelve200() throws Exception {
        when(eventoReconocimientoService.registrar(any(), any())).thenReturn(response(
                "primer_itinerario_generado", EstadoEnvioCertificacion.ENVIADO));

        mockMvc.perform(post("/api/reconocimiento/eventos")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario_id":"41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                 "evento_generado":"primer_itinerario_generado"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento_generado").value("primer_itinerario_generado"))
                .andExpect(jsonPath("$.estado_envio").value("ENVIADO"));

        verify(eventoReconocimientoService).registrar(any(), any());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void eventoFueraDeCatalogoDevuelve200SinErrorParaElCliente() throws Exception {
        when(eventoReconocimientoService.registrar(any(), any())).thenReturn(response(
                "evento_desconocido", EstadoEnvioCertificacion.FUERA_CATALOGO));

        mockMvc.perform(post("/api/reconocimiento/eventos")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario_id":"41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                 "evento_generado":"evento_desconocido"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado_envio").value("FUERA_CATALOGO"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void fallaDeCertificacionDevuelve200ConEventoEnCola() throws Exception {
        when(eventoReconocimientoService.registrar(any(), any())).thenReturn(response(
                "primer_itinerario_generado", EstadoEnvioCertificacion.PENDIENTE_REINTENTO));

        mockMvc.perform(post("/api/reconocimiento/eventos")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario_id":"41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                 "evento_generado":"primer_itinerario_generado"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado_envio").value("PENDIENTE_REINTENTO"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_GENERAL")
    void rolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/reconocimiento/eventos")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario_id":"41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                 "evento_generado":"primer_itinerario_generado"}"""))
                .andExpect(status().isForbidden());
    }

    private static Authentication principal() {
        return new UsernamePasswordAuthenticationToken(USUARIO_ID, null);
    }

    private static EventoReconocimientoResponseDTO response(
            String eventoGenerado,
            EstadoEnvioCertificacion estado) {
        return new EventoReconocimientoResponseDTO(
                UUID.randomUUID(),
                UUID.fromString(USUARIO_ID),
                eventoGenerado,
                FECHA_EVENTO,
                estado);
    }
}
