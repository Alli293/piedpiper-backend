package com.piedpiper.carbonhub.perfilpublico.controller;

import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;
import com.piedpiper.carbonhub.perfilpublico.service.EnlacePerfilService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoHuellaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoEvolucionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for PerfilPublicoController using jqwik + standalone MockMvc.
 * These tests verify structural invariants of the HTTP responses without requiring
 * the full Spring Boot context.
 */
class PerfilPublicoControllerPropertyTest {

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "nombreEmpresa", "logoUrl", "sectorIndustrial", "pais",
            "nivelEcologico", "fechaActualizacionNivel",
            "certificacionesVigentes", "insigniasActivas"
    );

    private static final Set<String> FORBIDDEN_PATTERNS_IN_ERROR = Set.of(
            "Exception", ".java", "at com.", "at org.", "at java.",
            "SELECT ", "INSERT ", "UPDATE ", "DELETE ", "FROM ",
            "NullPointerException", "StackTrace", "Caused by"
    );

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ========================================================================
    // Task 3.4 — Property 1: DTO exclusivamente público
    // ========================================================================

    /**
     * Validates: Requirements 1.1, 1.2
     *
     * For any active company with random data, the successful response JSON
     * SHALL contain ONLY the allowed keys: nombreEmpresa, logoUrl,
     * sectorIndustrial, pais, nivelEcologico, fechaActualizacionNivel,
     * certificacionesVigentes, insigniasActivas — no extra fields.
     */
    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 1: DTO exclusivamente público")
    void respuestaExitosaSoloContieneKeysPermitidas(
            @ForAll("slugValido") String slug,
            @ForAll("nombreEmpresa") String nombre,
            @ForAll("sectorIndustrial") String sector,
            @ForAll("pais") String pais,
            @ForAll("nivelEcologico") String nivel,
            @ForAll @IntRange(min = 0, max = 50) int certVigentes,
            @ForAll @IntRange(min = 0, max = 50) int insignias) throws Exception {

        // Arrange
        PerfilPublicoConsultaService mockService = mock(PerfilPublicoConsultaService.class);
        PerfilPublicoCertificacionesService mockCertService = mock(PerfilPublicoCertificacionesService.class);
        InsigniaEmpresaConsultaService mockInsigniaService = mock(InsigniaEmpresaConsultaService.class);
        EnlacePerfilService mockEnlaceService = mock(EnlacePerfilService.class);
        PerfilPublicoEvolucionService mockEvolucionService = mock(PerfilPublicoEvolucionService.class);
        PerfilPublicoHuellaService mockHuellaService = mock(PerfilPublicoHuellaService.class);

        PerfilPublicoResponseDTO dto = new PerfilPublicoResponseDTO(
                nombre, "https://cdn.example.com/logo.png", sector, pais,
                nivel, Instant.now(), certVigentes, insignias
        );
        when(mockService.obtenerPorSlug(slug)).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PerfilPublicoController(
                        mockService, mockCertService, mockInsigniaService,
                        mockEnlaceService, mockEvolucionService, mockHuellaService))
                .setControllerAdvice(new PerfilPublicoExceptionHandler())
                .build();

        // Act
        MvcResult result = mockMvc.perform(get("/api/perfil-publico/{slug}", slug))
                .andExpect(status().isOk())
                .andReturn();

        // Assert — parse JSON and verify only allowed keys
        String json = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(json, Map.class);
        Set<String> actualKeys = responseMap.keySet();

        assertThat(actualKeys)
                .as("Response JSON should only contain allowed public fields, got: %s", actualKeys)
                .isSubsetOf(ALLOWED_KEYS);

        // Verify no private/sensitive fields leak
        assertThat(actualKeys).doesNotContain("id", "email", "cedulaJuridica",
                "correoCorporativo", "cantidadEmpleados", "fechaRegistro",
                "estado", "password", "token", "slug");
    }

    // ========================================================================
    // Task 3.5 — Property 8: Respuestas de error con estructura segura
    // ========================================================================

    /**
     * Validates: Requirements 2.6, 3.3
     *
     * For various error conditions, the response body SHALL have ONLY the
     * "mensaje" field, no stack traces, no class names, no table names, no IPs.
     */
    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 8: Respuestas de error con estructura segura")
    void respuestasErrorConEstructuraSegura(
            @ForAll("errorCondition") ErrorCondition condition) throws Exception {

        // Arrange
        PerfilPublicoConsultaService mockService = mock(PerfilPublicoConsultaService.class);
        PerfilPublicoCertificacionesService mockCertService = mock(PerfilPublicoCertificacionesService.class);
        InsigniaEmpresaConsultaService mockInsigniaService = mock(InsigniaEmpresaConsultaService.class);
        EnlacePerfilService mockEnlaceService = mock(EnlacePerfilService.class);
        PerfilPublicoEvolucionService mockEvolucionService = mock(PerfilPublicoEvolucionService.class);
        PerfilPublicoHuellaService mockHuellaService = mock(PerfilPublicoHuellaService.class);

        switch (condition.type) {
            case PERFIL_NO_ENCONTRADO:
                when(mockService.obtenerPorSlug(condition.slug))
                        .thenThrow(new PerfilNoEncontradoException(condition.errorMessage));
                break;
            case DATA_ACCESS_ERROR:
                when(mockService.obtenerPorSlug(condition.slug))
                        .thenThrow(new DataAccessException("Connection refused to 192.168.1.100:5432 table empresa") {});
                break;
        }

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PerfilPublicoController(
                        mockService, mockCertService, mockInsigniaService,
                        mockEnlaceService, mockEvolucionService, mockHuellaService))
                .setControllerAdvice(new PerfilPublicoExceptionHandler())
                .build();

        // Act
        MvcResult result = mockMvc.perform(get("/api/perfil-publico/{slug}", condition.slug))
                .andReturn();

        // Assert — response must be JSON with only "mensaje" field
        String json = result.getResponse().getContentAsString();
        String contentType = result.getResponse().getContentType();

        assertThat(contentType)
                .as("Error response should be application/json")
                .contains("application/json");

        @SuppressWarnings("unchecked")
        Map<String, Object> errorMap = objectMapper.readValue(json, Map.class);

        assertThat(errorMap.keySet())
                .as("Error response should only contain 'mensaje' field, got: %s", errorMap.keySet())
                .containsExactly("mensaje");

        // Verify no internal data patterns in the mensaje value
        String mensaje = (String) errorMap.get("mensaje");
        assertThat(mensaje).isNotNull();
        assertThat(mensaje).isNotBlank();

        for (String forbidden : FORBIDDEN_PATTERNS_IN_ERROR) {
            assertThat(mensaje)
                    .as("Error message should not contain '%s'", forbidden)
                    .doesNotContain(forbidden);
        }

        // Verify no IP addresses in the message (pattern: digits.digits.digits.digits)
        assertThat(mensaje)
                .as("Error message should not contain IP addresses")
                .doesNotContainPattern("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

        // Verify no port patterns (host:port)
        assertThat(mensaje)
                .as("Error message should not contain port numbers")
                .doesNotContainPattern(":\\d{2,5}");
    }

    // ========================================================================
    // Custom types and providers
    // ========================================================================

    enum ErrorType {
        PERFIL_NO_ENCONTRADO, DATA_ACCESS_ERROR
    }

    static class ErrorCondition {
        final ErrorType type;
        final String slug;
        final String errorMessage;

        ErrorCondition(ErrorType type, String slug, String errorMessage) {
            this.type = type;
            this.slug = slug;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format("ErrorCondition{type=%s, slug='%s', msg='%s'}", type, slug, errorMessage);
        }
    }

    @Provide
    Arbitrary<ErrorCondition> errorCondition() {
        Arbitrary<String> slugs = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> s.matches("^[a-z0-9-]{1,30}$"));

        Arbitrary<ErrorCondition> perfilNoEncontrado = slugs.flatMap(slug ->
                Arbitraries.of(
                        "El perfil que buscas no existe o ya no está disponible.",
                        "Este perfil no está disponible en este momento."
                ).map(msg -> new ErrorCondition(ErrorType.PERFIL_NO_ENCONTRADO, slug, msg))
        );

        Arbitrary<ErrorCondition> dataAccessError = slugs.map(slug ->
                new ErrorCondition(ErrorType.DATA_ACCESS_ERROR, slug,
                        "Connection refused to 192.168.1.100:5432 table empresa")
        );

        return Arbitraries.oneOf(perfilNoEncontrado, dataAccessError);
    }

    // ========================================================================
    // Shared Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<String> slugValido() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> s.matches("^[a-z0-9-]{1,30}$"));
    }

    @Provide
    Arbitrary<String> nombreEmpresa() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withChars(' ', '.', ',')
                .ofMinLength(3)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> sectorIndustrial() {
        return Arbitraries.of("MANUFACTURA", "AGRICULTURA", "SERVICIOS",
                "TRANSPORTE", "ENERGIA", "TECNOLOGIA");
    }

    @Provide
    Arbitrary<String> pais() {
        return Arbitraries.of("Costa Rica", "México", "Colombia",
                "Argentina", "España", "Chile");
    }

    @Provide
    Arbitrary<String> nivelEcologico() {
        return Arbitraries.of("Sin nivel", "Bronce", "Plata", "Oro", "Platino");
    }
}
