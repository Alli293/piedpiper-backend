package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ecoruta.config.CertificacionApiKeyFilter;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaInsigniaService;
import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoCertificacionRequestDTO;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EcoRutaCertificacionEventoController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CertificacionApiKeyFilter.class,
        CorsConfig.class
})
@TestPropertySource(properties = {
        "certificacion.api-key=clave-prueba",
        "app.cors.allowed-origins=http://localhost:4200"
})
class EcoRutaCertificacionEventoControllerSecurityTest {

    private static final String HEADER_API_KEY = "X-Certificacion-Api-Key";
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
    void sinApiKeyDevuelve401YNoInvocaServicio() throws Exception {
        mockMvc.perform(post("/api/certificacion/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ecoRutaInsigniaService);
    }

    @Test
    void apiKeyValidaAutorizaEndpointEInvocaServicio() throws Exception {
        mockMvc.perform(post("/api/certificacion/eventos")
                        .header(HEADER_API_KEY, "clave-prueba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_VALIDO))
                .andExpect(status().isOk());

        verify(ecoRutaInsigniaService).evaluarYOtorgar(any(EventoCertificacionRequestDTO.class));
    }
}
