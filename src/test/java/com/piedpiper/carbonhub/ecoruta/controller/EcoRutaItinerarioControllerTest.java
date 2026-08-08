package com.piedpiper.carbonhub.ecoruta.controller;

import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.service.EcoRutaItinerarioService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EcoRutaItinerarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        CorsConfig.class
})
class EcoRutaItinerarioControllerTest {

    private static final String USUARIO_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private EcoRutaItinerarioService service;

    private ItinerarioResponseDTO respuesta() {
        ItinerarioResponseDTO response = new ItinerarioResponseDTO();
        response.setId(UUID.randomUUID());
        response.setCantidadDias(2);
        response.setFechaInicio(LocalDate.now().plusDays(10));
        response.setTipoViaje("INDIVIDUAL");
        response.setEstado("GENERADO");
        response.setVersion(1);
        response.setPuntuacionAmbientalPreliminar(new BigDecimal("82"));
        response.setFechaGeneracion(Instant.now());
        response.setGeneradoParcial(false);
        response.setDias(List.of());
        return response;
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postGenerarConPreferenciasGuardadasDevuelve201() throws Exception {
        when(service.generar(any(UUID.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/ecoruta/itinerarios/generar")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("GENERADO"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postGenerarSinPreferenciasGuardadasDevuelve404() throws Exception {
        when(service.generar(any(UUID.class)))
                .thenThrow(ApiException.recursoNoEncontrado(
                        "No has completado tus preferencias de viaje todavía."));

        mockMvc.perform(post("/api/ecoruta/itinerarios/generar")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void postGenerarConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/ecoruta/itinerarios/generar")
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postGenerarConRespuestaDeIaInvalidaDevuelve502ConMensajeControlado() throws Exception {
        when(service.generar(any(UUID.class))).thenThrow(ApiException.itinerarioRespuestaInvalida());

        mockMvc.perform(post("/api/ecoruta/itinerarios/generar")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("No fue posible generar una propuesta válida. Intenta nuevamente más tarde."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void postGenerarConLimiteExcedidoDevuelve429ConMensajeExacto() throws Exception {
        when(service.generar(any(UUID.class))).thenThrow(ApiException.itinerarioGeneracionesExcedidas());

        mockMvc.perform(post("/api/ecoruta/itinerarios/generar")
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("Has alcanzado el límite de itinerarios generados. Intenta de nuevo en una hora."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void getConItinerarioPropioDevuelve200() throws Exception {
        UUID itinerarioId = UUID.randomUUID();
        UUID usuarioId = UUID.fromString(USUARIO_ID);
        ItinerarioResponseDTO response = respuesta();
        response.setId(itinerarioId);
        when(service.perteneceAlUsuario(eq(itinerarioId), eq(usuarioId))).thenReturn(true);
        when(service.obtener(eq(itinerarioId), eq(usuarioId))).thenReturn(response);

        mockMvc.perform(get("/api/ecoruta/itinerarios/" + itinerarioId)
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itinerarioId.toString()));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void getConItinerarioInexistenteOAjenoDevuelve404() throws Exception {
        UUID itinerarioId = UUID.randomUUID();
        UUID usuarioId = UUID.fromString(USUARIO_ID);
        when(service.perteneceAlUsuario(eq(itinerarioId), eq(usuarioId))).thenReturn(true);
        when(service.obtener(eq(itinerarioId), eq(usuarioId)))
                .thenThrow(ApiException.recursoNoEncontrado("No fue posible encontrar el itinerario solicitado."));

        mockMvc.perform(get("/api/ecoruta/itinerarios/" + itinerarioId)
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No fue posible encontrar el itinerario solicitado."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_ADMINISTRADOR_EMPRESA")
    void getConRolNoAutorizadoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/ecoruta/itinerarios/" + UUID.randomUUID())
                        .principal(authentication("ROLE_ADMINISTRADOR_EMPRESA")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, authorities = "ROLE_USUARIO_INDIVIDUAL")
    void getConItinerarioAjenoDevuelve403() throws Exception {
        UUID itinerarioId = UUID.randomUUID();
        UUID usuarioId = UUID.fromString(USUARIO_ID);
        when(service.perteneceAlUsuario(eq(itinerarioId), eq(usuarioId))).thenReturn(false);

        mockMvc.perform(get("/api/ecoruta/itinerarios/" + itinerarioId)
                        .principal(authentication("ROLE_USUARIO_INDIVIDUAL")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("No tienes permiso para acceder a este itinerario."));
    }

    private TestingAuthenticationToken authentication(String authority) {
        return new TestingAuthenticationToken(USUARIO_ID, "password", authority);
    }
}
