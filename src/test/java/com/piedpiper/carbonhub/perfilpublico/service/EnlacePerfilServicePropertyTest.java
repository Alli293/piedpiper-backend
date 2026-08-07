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
}
