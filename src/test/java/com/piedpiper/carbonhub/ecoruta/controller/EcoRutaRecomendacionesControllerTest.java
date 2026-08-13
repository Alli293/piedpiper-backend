package com.piedpiper.carbonhub.ecoruta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.RecomendacionesResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.enums.TipoRecomendacion;
import com.piedpiper.carbonhub.ecoruta.service.RecomendacionAmbientalService;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EcoRutaRecomendacionesController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        CorsConfig.class
})
class EcoRutaRecomendacionesControllerTest {

    private static final String USUARIO_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID ITINERARIO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTIVIDAD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecomendacionAmbientalService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getRecomendacionesConEcoScoreValidoRetorna200ConCuerpoCorrecto() throws Exception {
        when(service.obtenerRecomendaciones(eq(ITINERARIO_ID), eq(UUID.fromString(USUARIO_ID))))
                .thenReturn(recomendacionesResponse());

        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/recomendaciones", ITINERARIO_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendaciones").isArray())
                .andExpect(jsonPath("$.recomendaciones.length()").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].tipo").value("ACTIVIDAD_ALTERNATIVA"))
                .andExpect(jsonPath("$.recomendaciones[0].actividadNombre").value("Canopy en Monteverde"))
                .andExpect(jsonPath("$.recomendaciones[0].incrementoEstimado").value(5.5))
                .andExpect(jsonPath("$.recomendaciones[0].alternativa.nombre")
                        .value("Senderismo en Reserva Biológica"))
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getRecomendacionesSinEcoScorePrevioRetorna422() throws Exception {
        when(service.obtenerRecomendaciones(eq(ITINERARIO_ID), eq(UUID.fromString(USUARIO_ID))))
                .thenThrow(ApiException.ecoScoreNoDisponible());

        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/recomendaciones", ITINERARIO_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Aún no se ha calculado un EcoScore para este itinerario."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getRecomendacionesConItinerarioAjenoRetorna403() throws Exception {
        when(service.obtenerRecomendaciones(eq(ITINERARIO_ID), eq(UUID.fromString(USUARIO_ID))))
                .thenThrow(ApiException.accesoDenegado("No tienes permiso para acceder a este itinerario."));

        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/recomendaciones", ITINERARIO_ID)
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para acceder a este itinerario."));
    }

    @Test
    void getRecomendacionesSinAutenticacionRetorna401() throws Exception {
        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/recomendaciones", ITINERARIO_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void putAplicarRecomendacionExitosaRetorna200ConItinerarioActualizado() throws Exception {
        SustitucionRequestDTO request = sustitucionRequest();
        ItinerarioResponseDTO itinerarioActualizado = itinerarioResponse();

        when(service.aplicarRecomendacion(eq(ITINERARIO_ID), eq(ACTIVIDAD_ID), any(SustitucionRequestDTO.class),
                eq(UUID.fromString(USUARIO_ID))))
                .thenReturn(itinerarioActualizado);

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/recomendaciones/{actividadId}/aplicar",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITINERARIO_ID.toString()))
                .andExpect(jsonPath("$.estado").value("GENERADO"));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void putAplicarRecomendacionConItinerarioAjenoRetorna403() throws Exception {
        SustitucionRequestDTO request = sustitucionRequest();

        when(service.aplicarRecomendacion(eq(ITINERARIO_ID), eq(ACTIVIDAD_ID), any(SustitucionRequestDTO.class),
                eq(UUID.fromString(USUARIO_ID))))
                .thenThrow(ApiException.accesoDenegado("No tienes permiso para acceder a este itinerario."));

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/recomendaciones/{actividadId}/aplicar",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para acceder a este itinerario."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void putAplicarRecomendacionConRequestInvalidoRetorna400() throws Exception {
        // nombre en blanco y ecoScore ausente violan @NotBlank/@NotNull de SustitucionRequestDTO
        SustitucionRequestDTO requestInvalido = sustitucionRequest();
        requestInvalido.setNombre("");
        requestInvalido.setEcoScore(null);

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/recomendaciones/{actividadId}/aplicar",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido))
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putAplicarRecomendacionSinAutenticacionRetorna401() throws Exception {
        SustitucionRequestDTO request = sustitucionRequest();

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/recomendaciones/{actividadId}/aplicar",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private RecomendacionesResponseDTO recomendacionesResponse() {
        AlternativaDTO alternativa = new AlternativaDTO();
        alternativa.setNombre("Senderismo en Reserva Biológica");
        alternativa.setDescripcion("Caminata guiada por bosque primario");
        alternativa.setEcoScore(85);
        alternativa.setCostoAproximado(new BigDecimal("15000"));
        alternativa.setMoneda("CRC");
        alternativa.setEstablecimientoRecomendado("Reserva Monteverde");
        alternativa.setDiferenciaAmbiental(20);
        alternativa.setMejorDesempeno(true);

        RecomendacionAmbientalDTO recomendacion = new RecomendacionAmbientalDTO(
                TipoRecomendacion.ACTIVIDAD_ALTERNATIVA,
                ACTIVIDAD_ID,
                "Canopy en Monteverde",
                "Sustituye \"Canopy en Monteverde\" por \"Senderismo en Reserva Biológica\".",
                new BigDecimal("5.5"),
                alternativa,
                "AVENTURA",
                "PUNTARENAS"
        );

        return new RecomendacionesResponseDTO(List.of(recomendacion), null);
    }

    private SustitucionRequestDTO sustitucionRequest() {
        SustitucionRequestDTO request = new SustitucionRequestDTO();
        request.setNombre("Senderismo en Reserva Biológica");
        request.setDescripcion("Caminata guiada por bosque primario");
        request.setCostoAproximado(new BigDecimal("15000"));
        request.setMoneda("CRC");
        request.setEstablecimientoRecomendado("Reserva Monteverde");
        request.setEcoScore(85);
        request.setCategoriaTuristica("AVENTURA");
        request.setProvincia("PUNTARENAS");
        return request;
    }

    private ItinerarioResponseDTO itinerarioResponse() {
        ItinerarioResponseDTO response = new ItinerarioResponseDTO();
        response.setId(ITINERARIO_ID);
        response.setCantidadDias(3);
        response.setFechaInicio(LocalDate.now().plusDays(10));
        response.setTipoViaje("INDIVIDUAL");
        response.setEstado("GENERADO");
        response.setVersion(1);
        response.setPuntuacionAmbientalPreliminar(new BigDecimal("78"));
        response.setFechaGeneracion(Instant.now());
        response.setGeneradoParcial(false);
        response.setDias(List.of());
        return response;
    }
}
