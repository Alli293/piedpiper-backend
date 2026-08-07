package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.repository.SlugHistoricoRepository;

import net.jqwik.api.*;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for slug validation in EnlacePerfilService.
 *
 * Validates: Requirements 1.5
 *
 * Property 8: Rechazo de slugs con formato inválido
 * For any string that does NOT match ^[a-z0-9-]{1,120}$ after lowercasing,
 * the service SHALL reject it without querying the database.
 */
class EnlacePerfilSlugValidationPropertyTest {

    private static final String BASE_URL = "https://carbonhub.app";
    private static final String OG_IMAGEN_FALLBACK = "https://carbonhub.app/images/og-default.png";
    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9-]{1,120}$");

    // ========================================================================
    // Property 8: Rechazo de slugs con formato inválido
    // ========================================================================

    /**
     * Validates: Requirements 1.5
     *
     * For any string containing characters outside [a-zA-Z0-9-],
     * the service SHALL throw PerfilNoEncontradoException without calling repos.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 8: Rechazo de slugs con formato inválido")
    void slugConCaracteresInvalidos_rechazaSinConsultarBD(
            @ForAll("slugConCaracteresInvalidos") String slugInvalido) {

        // Arrange
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        SlugHistoricoRepository slugHistoricoRepository = mock(SlugHistoricoRepository.class);
        QrGeneradorService qrGeneradorService = mock(QrGeneradorService.class);

        EnlacePerfilService service = new EnlacePerfilService(
                empresaRepository,
                slugHistoricoRepository,
                qrGeneradorService,
                BASE_URL,
                OG_IMAGEN_FALLBACK
        );

        // Precondition: after lowercasing, it must NOT match the valid pattern
        String normalized = slugInvalido.toLowerCase();
        Assume.that(!SLUG_VALIDO.matcher(normalized).matches());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerEnlacePerfil(slugInvalido))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        // Verify: NO repository calls were made
        verifyNoInteractions(empresaRepository);
        verifyNoInteractions(slugHistoricoRepository);
    }

    /**
     * Validates: Requirements 1.5
     *
     * For any string longer than 120 characters (even if composed of valid chars),
     * the service SHALL throw PerfilNoEncontradoException without calling repos.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 8: Rechazo de slugs con formato inválido")
    void slugDemasiadoLargo_rechazaSinConsultarBD(
            @ForAll("slugDemasiadoLargo") String slugLargo) {

        // Arrange
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        SlugHistoricoRepository slugHistoricoRepository = mock(SlugHistoricoRepository.class);
        QrGeneradorService qrGeneradorService = mock(QrGeneradorService.class);

        EnlacePerfilService service = new EnlacePerfilService(
                empresaRepository,
                slugHistoricoRepository,
                qrGeneradorService,
                BASE_URL,
                OG_IMAGEN_FALLBACK
        );

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerEnlacePerfil(slugLargo))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        // Verify: NO repository calls were made
        verifyNoInteractions(empresaRepository);
        verifyNoInteractions(slugHistoricoRepository);
    }

    /**
     * Validates: Requirements 1.5
     *
     * For empty string or null input, the service SHALL throw
     * PerfilNoEncontradoException without calling repos.
     */
    @Property(tries = 10)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 8: Rechazo de slugs con formato inválido")
    void slugVacioONull_rechazaSinConsultarBD(
            @ForAll("slugVacioONull") String slugVacio) {

        // Arrange
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        SlugHistoricoRepository slugHistoricoRepository = mock(SlugHistoricoRepository.class);
        QrGeneradorService qrGeneradorService = mock(QrGeneradorService.class);

        EnlacePerfilService service = new EnlacePerfilService(
                empresaRepository,
                slugHistoricoRepository,
                qrGeneradorService,
                BASE_URL,
                OG_IMAGEN_FALLBACK
        );

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerEnlacePerfil(slugVacio))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        // Verify: NO repository calls were made
        verifyNoInteractions(empresaRepository);
        verifyNoInteractions(slugHistoricoRepository);
    }

    // ========================================================================
    // Arbitraries — smart generators that produce truly invalid slugs
    // ========================================================================

    /**
     * Generates strings containing at least one character outside [a-zA-Z0-9-].
     * These include spaces, special chars, unicode, etc.
     */
    @Provide
    Arbitrary<String> slugConCaracteresInvalidos() {
        // Characters that are ALWAYS invalid even after lowercasing
        char[] invalidChars = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
                '+', '=', '{', '}', '[', ']', '|', '\\', '/', '?', '<', '>',
                ',', '.', '~', '`', ';', ':', '\'', '"', ' ', '\t', '\n',
                'ñ', 'á', 'é', 'ü', '©', '™', '€', '¥'};

        // Generate a base of valid chars with at least one invalid char injected
        Arbitrary<String> validPart = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(0)
                .ofMaxLength(50);

        Arbitrary<Character> invalidChar = Arbitraries.of(invalidChars);

        return Combinators.combine(validPart, invalidChar, validPart)
                .as((prefix, invalid, suffix) -> prefix + invalid + suffix)
                .filter(s -> !s.isEmpty());
    }

    /**
     * Generates strings longer than 120 characters using only valid chars [a-z0-9-].
     * After lowercasing, length stays the same, so they exceed the 120 limit.
     */
    @Provide
    Arbitrary<String> slugDemasiadoLargo() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(121)
                .ofMaxLength(300);
    }

    /**
     * Generates empty strings and null values that fail the slug regex.
     */
    @Provide
    Arbitrary<String> slugVacioONull() {
        return Arbitraries.of("", null);
    }
}
