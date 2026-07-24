package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ecoruta.service.InsigniaEcoRutaService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CertificacionInsigniaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class CertificacionInsigniaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsigniaEcoRutaService insigniaEcoRutaService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void eventoValidoInvocaServicioYDevuelve200() throws Exception {
        mockMvc.perform(post("/api/certificacion/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuario_id": "41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                  "evento_generado": "primer_itinerario_sostenible",
                                  "fecha_evento": "2026-07-15T20:32:00Z"
                                }
                                """))
                .andExpect(status().isOk());

        verify(insigniaEcoRutaService).evaluarYOtorgar(any(EventoCertificacionRequestDTO.class));
    }

    @Test
    void excepcionInesperadaDelServicioDevuelve500() throws Exception {
        doThrow(new RuntimeException("fallo silencioso")).when(insigniaEcoRutaService)
                .evaluarYOtorgar(any());

        mockMvc.perform(post("/api/certificacion/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuario_id": "41ce47ab-a46c-4306-8c46-2688dc97fa73",
                                  "evento_generado": "primer_itinerario_sostenible",
                                  "fecha_evento": "2026-07-15T20:32:00Z"
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }
}
