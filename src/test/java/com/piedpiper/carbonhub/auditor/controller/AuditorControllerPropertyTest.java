package com.piedpiper.carbonhub.auditor.controller;

import com.piedpiper.carbonhub.auditor.service.AuditorDirectorioService;
import com.piedpiper.carbonhub.auditor.service.PerfilPublicoAuditorService;
import com.piedpiper.carbonhub.exceptions.GlobalExceptionHandler;

import net.jqwik.api.*;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for AuditorController using jqwik + standalone MockMvc.
 * Validates: Requirements 3.1
 */
class AuditorControllerPropertyTest {

    private final AuditorDirectorioService auditorDirectorioService = mock(AuditorDirectorioService.class);
    private final PerfilPublicoAuditorService perfilPublicoAuditorService = mock(PerfilPublicoAuditorService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuditorController(auditorDirectorioService, perfilPublicoAuditorService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    // ========================================================================
    // Property 5: Valores inválidos de auditorId retornan HTTP 400
    // ========================================================================

    /**
     * Validates: Requirements 3.1
     *
     * For any string that is NOT a valid UUID, the endpoint GET /api/auditores/{auditorId}
     * SHALL return HTTP 400 because Spring cannot convert the path variable to UUID,
     * triggering MethodArgumentTypeMismatchException handled by GlobalExceptionHandler.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor Property 5")
    void valoresInvalidosDeAuditorIdRetornanHttp400(
            @ForAll("stringNoUuid") String invalidAuditorId) throws Exception {

        mockMvc.perform(get("/api/auditores/{auditorId}", invalidAuditorId))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> stringNoUuid() {
        Arbitrary<String> tooShort = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(10);

        Arbitrary<String> invalidChars = Arbitraries.strings()
                .withCharRange('g', 'z')
                .withChars('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', ' ', '+', '=')
                .ofMinLength(5)
                .ofMaxLength(40);

        Arbitrary<String> wrongFormat = Arbitraries.strings()
                .withCharRange('0', '9')
                .withCharRange('a', 'f')
                .withChars('-')
                .ofMinLength(30)
                .ofMaxLength(50)
                .filter(s -> !isValidUuid(s));

        Arbitrary<String> veryLong = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(50)
                .ofMaxLength(200);

        Arbitrary<String> numericOnly = Arbitraries.integers()
                .between(-1000, 1000)
                .map(String::valueOf);

        Arbitrary<String> withSpaces = Arbitraries.of(
                "not a uuid",
                "hello world",
                "123 456",
                " ",
                "  spaces  "
        );

        Arbitrary<String> singleChars = Arbitraries.of(
                "x", "0", "-", "abc", "12345"
        );

        return Arbitraries.oneOf(
                tooShort, invalidChars, wrongFormat, veryLong,
                numericOnly, withSpaces, singleChars
        );
    }

    private boolean isValidUuid(String value) {
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
