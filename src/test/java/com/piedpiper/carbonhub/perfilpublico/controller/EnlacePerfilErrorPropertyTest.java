package com.piedpiper.carbonhub.perfilpublico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.service.EnlacePerfilService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoCertificacionesService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoConsultaService;
import com.piedpiper.carbonhub.insignia.service.InsigniaEmpresaConsultaService;
import com.piedpiper.carbonhub.perfilpublico.service.PerfilPublicoHuellaService;

import net.jqwik.api.*;

import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Property-based test for Property 11: Respuestas de error no exponen datos internos.
 *
 * Validates: Requirements 9.3
 *
 * For any error response (4xx or 5xx) from the /compartir endpoint, the body
 * SHALL NOT contain stack traces, Java class names, database table/column names,
 * or infrastructure data (IPs, internal ports).
 */
class EnlacePerfilErrorPropertyTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Patterns that must NEVER appear in error responses.
     */
    private static final Set<String> FORBIDDEN_SUBSTRINGS = Set.of(
            "at com.",                      // stack trace lines
            "com.piedpiper",                // Java package names
            "slug_historico",               // DB table name
            "empresas",                     // DB table name
            ".java:",                       // Java source file references
            "NullPointerException",         // exception class names
            "DataAccessException",          // exception class names
            "StackTrace",                   // stack trace keyword
            "Caused by:",                   // chained exception indicator
            "org.springframework",          // Spring framework internals
            "org.hibernate",                // Hibernate internals
            "java.sql",                     // JDBC internals
            "SELECT ",                      // SQL queries
            "INSERT ",                      // SQL queries
            "UPDATE ",                      // SQL queries
            "DELETE ",                      // SQL queries
            "FROM ",                        // SQL queries
            "WHERE "                        // SQL queries
    );

    /** Regex for IP addresses like 192.168.1.100 */
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    /** Regex for internal port numbers like :5432, :8080 */
    private static final Pattern PORT_PATTERN =
            Pattern.compile(":\\d{2,5}");

    // ========================================================================
    // Property 11: Respuestas de error no exponen datos internos
    // ========================================================================

    /**
     * Validates: Requirements 9.3
     *
     * For any error scenario (PerfilNoEncontradoException, DataAccessException,
     * generic RuntimeException) triggered on the /compartir endpoint, the response
     * body SHALL NOT contain stack traces, Java class names, DB table/column names,
     * IPs, or internal ports.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 11: Respuestas de error no exponen datos internos")
    void errorResponses_noExponenDatosInternos(
            @ForAll("errorScenario") ErrorScenario scenario) throws Exception {

        // Arrange
        PerfilPublicoConsultaService mockConsultaService = mock(PerfilPublicoConsultaService.class);
        PerfilPublicoCertificacionesService mockCertService = mock(PerfilPublicoCertificacionesService.class);
        InsigniaEmpresaConsultaService mockInsigniaService = mock(InsigniaEmpresaConsultaService.class);
        EnlacePerfilService mockEnlaceService = mock(EnlacePerfilService.class);
        PerfilPublicoHuellaService mockHuellaService = mock(PerfilPublicoHuellaService.class);

        // Configure mock to throw the appropriate exception
        switch (scenario.type) {
            case PERFIL_NO_ENCONTRADO:
                when(mockEnlaceService.obtenerEnlacePerfil(scenario.slug))
                        .thenThrow(new PerfilNoEncontradoException(scenario.exceptionMessage));
                break;
            case DATA_ACCESS_ERROR:
                when(mockEnlaceService.obtenerEnlacePerfil(scenario.slug))
                        .thenThrow(new DataAccessException(scenario.exceptionMessage) {});
                break;
        }

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PerfilPublicoController(
                        mockConsultaService, mockCertService,
                        mockInsigniaService, mockEnlaceService, mockHuellaService))
                .setControllerAdvice(new PerfilPublicoExceptionHandler())
                .build();

        // Act
        MvcResult result = mockMvc.perform(
                get("/api/perfil-publico/{slug}/compartir", scenario.slug))
                .andReturn();

        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();

        // Assert — status is 4xx or 5xx
        assertThat(status)
                .as("Error response should have 4xx or 5xx status, got %d", status)
                .isGreaterThanOrEqualTo(400);

        // Assert — response body does not contain forbidden substrings
        for (String forbidden : FORBIDDEN_SUBSTRINGS) {
            assertThat(responseBody)
                    .as("Error response should NOT contain '%s'. Full body: %s",
                            forbidden, responseBody)
                    .doesNotContain(forbidden);
        }

        // Assert — response body does not contain IP addresses
        assertThat(IP_PATTERN.matcher(responseBody).find())
                .as("Error response should NOT contain IP addresses. Body: %s", responseBody)
                .isFalse();

        // Assert — response body does not contain internal port patterns
        assertThat(PORT_PATTERN.matcher(responseBody).find())
                .as("Error response should NOT contain internal port numbers. Body: %s", responseBody)
                .isFalse();

        // Assert — only contains the expected "mensaje" field
        if (!responseBody.isBlank()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);

            assertThat(errorMap.keySet())
                    .as("Error response should only have 'mensaje' field, got: %s", errorMap.keySet())
                    .containsExactly("mensaje");

            String mensaje = (String) errorMap.get("mensaje");
            assertThat(mensaje).isNotNull();
            assertThat(mensaje).isNotBlank();
        }
    }

    // ========================================================================
    // Custom types and providers
    // ========================================================================

    enum ErrorType {
        PERFIL_NO_ENCONTRADO,
        DATA_ACCESS_ERROR
    }

    static class ErrorScenario {
        final ErrorType type;
        final String slug;
        final String exceptionMessage;

        ErrorScenario(ErrorType type, String slug, String exceptionMessage) {
            this.type = type;
            this.slug = slug;
            this.exceptionMessage = exceptionMessage;
        }

        @Override
        public String toString() {
            return String.format("ErrorScenario{type=%s, slug='%s', exMsg='%s'}",
                    type, slug, exceptionMessage);
        }
    }

    @Provide
    Arbitrary<ErrorScenario> errorScenario() {
        Arbitrary<String> slugs = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> s.matches("^[a-z0-9-]{1,30}$"));

        // Simulate PerfilNoEncontradoException with the safe user-facing message
        Arbitrary<ErrorScenario> perfilNoEncontrado = slugs.map(slug ->
                new ErrorScenario(
                        ErrorType.PERFIL_NO_ENCONTRADO,
                        slug,
                        "El perfil que buscas no existe o ya no está disponible.")
        );

        // Simulate DataAccessException with internal details that should NOT leak
        Arbitrary<ErrorScenario> dataAccessErrors = slugs.flatMap(slug ->
                Arbitraries.of(
                        "Connection refused to 192.168.1.100:5432 on table empresas",
                        "Could not open JPA EntityManager for transaction; nested: org.hibernate.exception.JDBCConnectionException",
                        "PreparedStatementCallback; SQL [SELECT * FROM slug_historico WHERE slug_anterior = ?]; Connection reset",
                        "StatementCallback; bad SQL grammar [INSERT INTO empresas (slug) VALUES (?)]; nested: java.sql.SQLSyntaxErrorException",
                        "DataAccessResourceFailureException: Unable to acquire JDBC Connection from 10.0.0.5:5432",
                        "could not extract ResultSet; SQL [n/a]; nested exception is org.hibernate.exception.SQLGrammarException: could not extract ResultSet"
                ).map(msg -> new ErrorScenario(ErrorType.DATA_ACCESS_ERROR, slug, msg))
        );

        return Arbitraries.oneOf(perfilNoEncontrado, dataAccessErrors);
    }
}
