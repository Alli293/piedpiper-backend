package com.piedpiper.carbonhub.ecoruta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.auth.config.CorsConfig;
import com.piedpiper.carbonhub.auth.config.JwtAuthenticationFilter;
import com.piedpiper.carbonhub.auth.config.SecurityConfig;
import com.piedpiper.carbonhub.auth.service.JwtService;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ComparacionResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.service.ComparacionAlternativasService;
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

@WebMvcTest(controllers = EcoRutaAlternativasController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        CorsConfig.class
})
class EcoRutaAlternativasControllerTest {

    private static final String USUARIO_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID ITINERARIO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTIVIDAD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ComparacionAlternativasService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getAlternativasRetorna200ConCuerpoCorrecto() throws Exception {
        ComparacionResponseDTO response = comparacionResponse();
        when(service.obtenerAlternativas(eq(ITINERARIO_ID), eq(ACTIVIDAD_ID), eq(UUID.fromString(USUARIO_ID))))
                .thenReturn(response);

        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/actividades/{actividadId}/alternativas",
                        ITINERARIO_ID, ACTIVIDAD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividadOriginalNombre").value("Canopy en Monteverde"))
                .andExpect(jsonPath("$.ecoScoreOriginal").value(65))
                .andExpect(jsonPath("$.categoriaTuristica").value("AVENTURA"))
                .andExpect(jsonPath("$.provincia").value("PUNTARENAS"))
                .andExpect(jsonPath("$.alternativas").isArray())
                .andExpect(jsonPath("$.alternativas.length()").value(2))
                .andExpect(jsonPath("$.alternativas[0].nombre").value("Senderismo en Reserva Biológica"))
                .andExpect(jsonPath("$.alternativas[0].ecoScore").value(85))
                .andExpect(jsonPath("$.alternativas[0].diferenciaAmbiental").value(20))
                .andExpect(jsonPath("$.alternativas[0].mejorDesempeno").value(true))
                .andExpect(jsonPath("$.alternativas[1].nombre").value("Kayak en Golfo Dulce"))
                .andExpect(jsonPath("$.alternativas[1].mejorDesempeno").value(false))
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void getAlternativasConItinerarioAjenoRetorna403() throws Exception {
        when(service.obtenerAlternativas(eq(ITINERARIO_ID), eq(ACTIVIDAD_ID), eq(UUID.fromString(USUARIO_ID))))
                .thenThrow(ApiException.accesoDenegado("No tienes permiso para acceder a este itinerario."));

        mockMvc.perform(get("/api/ecoruta/itinerarios/{id}/actividades/{actividadId}/alternativas",
                        ITINERARIO_ID, ACTIVIDAD_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para acceder a este itinerario."));
    }

    @Test
    @WithMockUser(username = USUARIO_ID, roles = "USUARIO_INDIVIDUAL")
    void putSustitucionExitosaRetorna200ConItinerarioActualizado() throws Exception {
        SustitucionRequestDTO request = sustitucionRequest();
        ItinerarioResponseDTO itinerarioActualizado = itinerarioResponse();

        when(service.sustituirActividad(eq(ITINERARIO_ID), eq(ACTIVIDAD_ID), any(SustitucionRequestDTO.class),
                eq(UUID.fromString(USUARIO_ID))))
                .thenReturn(itinerarioActualizado);

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/actividades/{actividadId}/sustituir",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(USUARIO_ID).roles("USUARIO_INDIVIDUAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITINERARIO_ID.toString()))
                .andExpect(jsonPath("$.estado").value("GENERADO"))
                .andExpect(jsonPath("$.cantidadDias").value(3));
    }

    @Test
    void putSustitucionSinAutenticacionRetorna401() throws Exception {
        SustitucionRequestDTO request = sustitucionRequest();

        mockMvc.perform(put("/api/ecoruta/itinerarios/{id}/actividades/{actividadId}/sustituir",
                        ITINERARIO_ID, ACTIVIDAD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private ComparacionResponseDTO comparacionResponse() {
        AlternativaDTO alt1 = new AlternativaDTO();
        alt1.setNombre("Senderismo en Reserva Biológica");
        alt1.setDescripcion("Caminata guiada por bosque primario");
        alt1.setEcoScore(85);
        alt1.setCostoAproximado(new BigDecimal("15000"));
        alt1.setMoneda("CRC");
        alt1.setEstablecimientoRecomendado("Reserva Monteverde");
        alt1.setDiferenciaAmbiental(20);
        alt1.setMejorDesempeno(true);

        AlternativaDTO alt2 = new AlternativaDTO();
        alt2.setNombre("Kayak en Golfo Dulce");
        alt2.setDescripcion("Tour de kayak ecológico");
        alt2.setEcoScore(75);
        alt2.setCostoAproximado(new BigDecimal("25000"));
        alt2.setMoneda("CRC");
        alt2.setEstablecimientoRecomendado("EcoKayak CR");
        alt2.setDiferenciaAmbiental(10);
        alt2.setMejorDesempeno(false);

        return new ComparacionResponseDTO(
                "Canopy en Monteverde",
                65,
                "AVENTURA",
                "PUNTARENAS",
                List.of(alt1, alt2),
                null
        );
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
