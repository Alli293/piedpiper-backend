package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.EnlacePerfilDTO;
import com.piedpiper.carbonhub.perfilpublico.repository.SlugHistoricoRepository;

import net.jqwik.api.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for EnlacePerfilService using jqwik.
 * Each property verifies a universal invariant across many generated inputs.
 */
class EnlacePerfilServicePropertyTest {

    private static final String BASE_URL = "https://carbonhub.app";
    private static final String OG_IMAGEN_FALLBACK = "https://carbonhub.app/images/og-default.png";

    // ========================================================================
    // Property 4: Formato de ogTitulo
    // ========================================================================

    /**
     * Validates: Requirements 4.1
     *
     * For any active company with a non-empty nombreEmpresa, the ogTitulo field
     * SHALL be exactly "{nombreEmpresa} — Perfil de Reputación Ecológica | CarbonHub".
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 4: Formato de ogTitulo")
    void ogTitulo_sigueFormatoExacto(
            @ForAll("nombreEmpresaArbitrario") String nombreEmpresa,
            @ForAll("slugValido") String slug) {

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

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombreEmpresa)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl("https://example.com/logo.png")
                .nivelEcologico("ORO")
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        when(empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,test");

        // Act
        EnlacePerfilDTO dto = service.obtenerEnlacePerfil(slug);

        // Assert
        String expectedOgTitulo = nombreEmpresa + " — Perfil de Reputación Ecológica | CarbonHub";
        assertThat(dto.getOgTitulo())
                .as("ogTitulo for empresa '%s' should follow exact format", nombreEmpresa)
                .isEqualTo(expectedOgTitulo);
    }

    // ========================================================================
    // Property 5: Formato de ogDescripcion
    // ========================================================================

    /**
     * Validates: Requirements 4.2
     *
     * For any active company with nivelEcologico (including null) and nombreEmpresa,
     * the ogDescripcion field SHALL be exactly:
     * "Nivel ecológico: {nivelEcologico}. Consulta el desempeño ambiental verificado de {nombreEmpresa}."
     * When nivelEcologico is null or blank, it displays "Sin nivel".
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 5: Formato de ogDescripcion")
    void ogDescripcion_sigueFormatoExacto(
            @ForAll("nombreEmpresaArbitrario") String nombreEmpresa,
            @ForAll("slugValido") String slug,
            @ForAll("nivelEcologicoArbitrario") String nivelEcologico) {

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

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombreEmpresa)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl("https://example.com/logo.png")
                .nivelEcologico(nivelEcologico)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        when(empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,test");

        // Act
        EnlacePerfilDTO dto = service.obtenerEnlacePerfil(slug);

        // Assert — determine expected nivel display
        String nivelEsperado = (nivelEcologico == null || nivelEcologico.isBlank())
                ? "Sin nivel"
                : nivelEcologico;

        String expectedOgDescripcion = "Nivel ecológico: " + nivelEsperado
                + ". Consulta el desempeño ambiental verificado de " + nombreEmpresa + ".";

        assertThat(dto.getOgDescripcion())
                .as("ogDescripcion for empresa '%s' with nivel '%s' should follow exact format",
                        nombreEmpresa, nivelEcologico)
                .isEqualTo(expectedOgDescripcion);
    }

    // ========================================================================
    // Property 6: Resolución de ogImagen según logoUrl
    // ========================================================================

    /**
     * Validates: Requirements 4.3, 4.4
     *
     * For any active company, if logoUrl is not null and not blank then ogImagen
     * SHALL equal logoUrl; if logoUrl is null or blank then ogImagen SHALL equal
     * the configured ogImagenFallback.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 6: Resolución de ogImagen según logoUrl")
    void ogImagen_resuelveSeguLogoUrl(
            @ForAll("slugValido") String slug,
            @ForAll("nombreEmpresaArbitrario") String nombreEmpresa,
            @ForAll("logoUrlArbitrario") String logoUrl) {

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

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombreEmpresa)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl(logoUrl)
                .nivelEcologico("PLATA")
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        when(empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,test");

        // Act
        EnlacePerfilDTO dto = service.obtenerEnlacePerfil(slug);

        // Assert
        if (logoUrl != null && !logoUrl.isBlank()) {
            assertThat(dto.getOgImagen())
                    .as("ogImagen should be logoUrl when logoUrl is present: '%s'", logoUrl)
                    .isEqualTo(logoUrl);
        } else {
            assertThat(dto.getOgImagen())
                    .as("ogImagen should be fallback when logoUrl is null or blank")
                    .isEqualTo(OG_IMAGEN_FALLBACK);
        }
    }

    // ========================================================================
    // Property 7: Estructura del fragmento HTML incrustable
    // ========================================================================

    /**
     * Validates: Requirements 5.1, 5.2, 5.3
     *
     * For any active company, the codigoIncrustar field SHALL contain:
     * (a) an <a> element whose href points to the canonical URL,
     * (b) visible text including nombreEmpresa and "Perfil verificado en CarbonHub",
     * (c) style= attributes for inline CSS, and
     * (d) SHALL NOT contain <script> elements, document.cookie references,
     *     nor external dependencies.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-69-enlace-comparticion-perfil, Property 7: Estructura del fragmento HTML incrustable")
    void codigoIncrustar_tieneEstructuraHTMLCorrecta(
            @ForAll("nombreEmpresaArbitrario") String nombreEmpresa,
            @ForAll("slugValido") String slug) {

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

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombreEmpresa)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl("https://example.com/logo.png")
                .nivelEcologico("PLATA")
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();

        when(empresaRepository.findBySlugAndEstado(slug, EstadoEmpresa.ACTIVO))
                .thenReturn(Optional.of(empresa));
        when(qrGeneradorService.generarQrBase64(any())).thenReturn("data:image/png;base64,test");

        // Act
        EnlacePerfilDTO dto = service.obtenerEnlacePerfil(slug);
        String codigo = dto.getCodigoIncrustar();

        // Assert
        String urlCanonica = BASE_URL + "/empresa/" + slug + "/reputacion";

        // (a) Contains <a> tag with correct href pointing to urlCanonica
        assertThat(codigo)
                .as("codigoIncrustar should contain an <a element")
                .contains("<a ");
        assertThat(codigo)
                .as("codigoIncrustar href should point to the canonical URL")
                .contains("href=\"" + urlCanonica + "\"");

        // (b) Contains nombreEmpresa and "Perfil verificado en CarbonHub"
        assertThat(codigo)
                .as("codigoIncrustar should contain the company name")
                .contains(nombreEmpresa);
        assertThat(codigo)
                .as("codigoIncrustar should contain 'Perfil verificado en CarbonHub'")
                .contains("Perfil verificado en CarbonHub");

        // (c) Contains style= for inline CSS
        assertThat(codigo)
                .as("codigoIncrustar should have inline CSS via style attribute")
                .contains("style=");

        // (d) Does NOT contain <script> or document.cookie
        assertThat(codigo)
                .as("codigoIncrustar must not contain <script> elements")
                .doesNotContainIgnoringCase("<script");
        assertThat(codigo)
                .as("codigoIncrustar must not contain document.cookie references")
                .doesNotContain("document.cookie");
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<String> nombreEmpresaArbitrario() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '&', '-', 'á', 'é', 'í', 'ó', 'ú', 'ñ', 'Ñ')
                .ofMinLength(1)
                .ofMaxLength(100)
                .filter(s -> !s.isBlank());
    }

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
    Arbitrary<String> nivelEcologicoArbitrario() {
        return Arbitraries.oneOf(
                Arbitraries.of("BRONCE", "PLATA", "ORO", "PLATINO", "SIN_NIVEL"),
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   ")
        );
    }

    @Provide
    Arbitrary<String> logoUrlArbitrario() {
        Arbitrary<String> validUrls = Arbitraries.of(
                "https://example.com/logo.png",
                "https://cdn.carbonhub.app/logos/empresa-123.png",
                "https://storage.googleapis.com/bucket/logo.jpg",
                "https://my-company.com/assets/brand/logo-wide.png"
        );
        Arbitrary<String> nullValue = Arbitraries.just(null);
        Arbitrary<String> emptyStrings = Arbitraries.of("", "   ", "\t", "\n");

        return Arbitraries.oneOf(validUrls, nullValue, emptyStrings);
    }
}
