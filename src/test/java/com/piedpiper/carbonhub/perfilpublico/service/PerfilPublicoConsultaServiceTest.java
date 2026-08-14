package com.piedpiper.carbonhub.perfilpublico.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.empresa.models.enums.EstadoEmpresa;
import com.piedpiper.carbonhub.empresa.models.enums.SectorIndustrial;
import com.piedpiper.carbonhub.empresa.repository.EmpresaRepository;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;
import com.piedpiper.carbonhub.ima.repository.ImaSnapshotRepository;
import com.piedpiper.carbonhub.insignia.repository.InsigniaEmpresaRepository;
import com.piedpiper.carbonhub.perfilpublico.exceptions.PerfilNoEncontradoException;
import com.piedpiper.carbonhub.perfilpublico.models.dtos.PerfilPublicoResponseDTO;
import com.piedpiper.carbonhub.perfilpublico.service.SlugResolverService;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for PerfilPublicoConsultaService combining jqwik property-based tests
 * and JUnit 5 unit tests.
 */
@ExtendWith(MockitoExtension.class)
class PerfilPublicoConsultaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private CertificacionRepository certificacionRepository;

    @Mock
    private InsigniaEmpresaRepository insigniaEmpresaRepository;

    @Mock
    private ImaSnapshotRepository imaSnapshotRepository;

    @Mock
    private SlugResolverService slugResolver;

    @InjectMocks
    private PerfilPublicoConsultaService service;

    @BeforeEach
    void configurar() {
        lenient().when(insigniaEmpresaRepository.findByEmpresaIdOrderByFechaObtencionDesc(any()))
                .thenReturn(java.util.List.of());
    }

    // ========================================================================
    // Unit Tests (JUnit 5) — Task 2.6
    // ========================================================================

    @Test
    @DisplayName("Empresa activa → DTO correcto con todos los campos")
    void obtenerPorSlug_empresaActiva_retornaDtoCorrecto() {
        // Arrange
        Empresa empresa = buildEmpresaActiva("eco-verde", "EcoVerde S.A.", "Oro");
        when(slugResolver.resolver("eco-verde")).thenReturn(empresa);
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(List.of(buildCertificacion(empresa, LocalDate.now().plusDays(30))));

        // Act
        PerfilPublicoResponseDTO dto = service.obtenerPorSlug("eco-verde");

        // Assert
        assertThat(dto.getNombreEmpresa()).isEqualTo("EcoVerde S.A.");
        assertThat(dto.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.png");
        assertThat(dto.getSectorIndustrial()).isEqualTo("MANUFACTURA");
        assertThat(dto.getPais()).isEqualTo("Costa Rica");
        assertThat(dto.getNivelEcologico()).isEqualTo("Oro");
        assertThat(dto.getCertificacionesVigentes()).isEqualTo(1);
        assertThat(dto.getInsigniasActivas()).isEqualTo(0);
    }

    @Test
    @DisplayName("Empresa inactiva → PerfilNoEncontradoException (mismo mensaje que inexistente)")
    void obtenerPorSlug_empresaInactiva_lanzaExcepcion() {
        when(slugResolver.resolver("empresa-inactiva"))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        assertThatThrownBy(() -> service.obtenerPorSlug("empresa-inactiva"))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");
    }

    @Test
    @DisplayName("Empresa inexistente → PerfilNoEncontradoException con mensaje correcto")
    void obtenerPorSlug_empresaInexistente_lanzaExcepcion() {
        // Arrange
        when(slugResolver.resolver("slug-inexistente"))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerPorSlug("slug-inexistente"))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");
    }

    @Test
    @DisplayName("Slug inválido (caracteres especiales) → PerfilNoEncontradoException")
    void obtenerPorSlug_slugInvalido_lanzaExcepcion() {
        when(slugResolver.resolver("empresa@#$%"))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerPorSlug("empresa@#$%"))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");
    }

    @Test
    @DisplayName("NivelEcologico null y sin snapshot IMA → 'Sin nivel' en DTO")
    void obtenerPorSlug_nivelEcologicoNull_retornaSinNivel() {
        // Arrange
        Empresa empresa = buildEmpresaActiva("empresa-sin-nivel", "Empresa Sin Nivel", null);
        when(slugResolver.resolver("empresa-sin-nivel")).thenReturn(empresa);
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        PerfilPublicoResponseDTO dto = service.obtenerPorSlug("empresa-sin-nivel");

        // Assert
        assertThat(dto.getNivelEcologico()).isEqualTo("Sin nivel");
        assertThat(dto.getFechaActualizacionNivel()).isNull();
    }

    @Test
    @DisplayName("NivelEcologico null con snapshot IMA → nivel derivado del IMA y su fecha")
    void obtenerPorSlug_nivelEcologicoNullConSnapshotIma_derivaNivelDeIma() {
        // Arrange
        Empresa empresa = buildEmpresaActiva("empresa-ima", "Empresa Ima", null);
        Instant calculatedAt = Instant.parse("2026-03-01T00:00:00Z");
        ImaSnapshot snapshot = ImaSnapshot.builder()
                .empresaId(empresa.getId())
                .anio(2026)
                .mes(3)
                .ima(new java.math.BigDecimal("82.0"))
                .cobertura(new java.math.BigDecimal("100.0"))
                .consistencia(new java.math.BigDecimal("100.0"))
                .parcial(false)
                .calculatedAt(calculatedAt)
                .build();

        when(slugResolver.resolver("empresa-ima")).thenReturn(empresa);
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(imaSnapshotRepository.findFirstByEmpresaIdOrderByAnioDescMesDesc(empresa.getId()))
                .thenReturn(Optional.of(snapshot));

        // Act
        PerfilPublicoResponseDTO dto = service.obtenerPorSlug("empresa-ima");

        // Assert — IMA 82.0 cae en el umbral de Oro (>= 75 y < 90)
        assertThat(dto.getNivelEcologico()).isEqualTo("Oro");
        assertThat(dto.getFechaActualizacionNivel()).isEqualTo(calculatedAt);
    }

    @Test
    @DisplayName("NivelEcologico con valor manual explícito → se respeta tal cual, sin consultar IMA")
    void obtenerPorSlug_nivelEcologicoManual_ignoraIma() {
        // Arrange
        Empresa empresa = buildEmpresaActiva("empresa-manual", "Empresa Manual", "Oro");
        when(slugResolver.resolver("empresa-manual")).thenReturn(empresa);
        when(certificacionRepository
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        PerfilPublicoResponseDTO dto = service.obtenerPorSlug("empresa-manual");

        // Assert
        assertThat(dto.getNivelEcologico()).isEqualTo("Oro");
        verify(imaSnapshotRepository, never()).findFirstByEmpresaIdOrderByAnioDescMesDesc(any());
    }

    // ========================================================================
    // Property Tests (jqwik) — Tasks 2.2, 2.3, 2.4, 2.5
    // ========================================================================

    // --- Task 2.2: Property 2 — Nivel ecológico por defecto ---

    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 2: Nivel ecológico por defecto")
    void nivelEcologicoNullOVacio_retornaSinNivel(
            @ForAll("nivelEcologicoVacioONull") String nivelEcologico,
            @ForAll("slugValido") String slug) {

        // Arrange
        EmpresaRepository mockRepo = mock(EmpresaRepository.class);
        CertificacionRepository mockCertRepo = mock(CertificacionRepository.class);
        SlugResolverService mockSlugResolver = mock(SlugResolverService.class);
        PerfilPublicoConsultaService svc = new PerfilPublicoConsultaService(mockRepo, mockCertRepo, mock(InsigniaEmpresaRepository.class), mock(ImaSnapshotRepository.class), mockSlugResolver);

        Empresa empresa = buildEmpresaActiva(slug, "Empresa Test", nivelEcologico);
        when(mockSlugResolver.resolver(slug)).thenReturn(empresa);
        when(mockCertRepo
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        PerfilPublicoResponseDTO dto = svc.obtenerPorSlug(slug);

        // Assert
        assertThat(dto.getNivelEcologico()).isEqualTo("Sin nivel");
    }

    @Provide
    Arbitrary<String> nivelEcologicoVacioONull() {
        return Arbitraries.of(
                null,
                "",
                " ",
                "  ",
                "   ",
                "\t",
                "\n",
                " \t\n "
        );
    }

    // --- Task 2.3: Property 4 — Búsqueda insensible a mayúsculas ---

    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 4: Búsqueda insensible a mayúsculas")
    void busquedaInsensibleAMayusculas_retornaMismoDto(
            @ForAll("slugValidoParaCapitalizacion") String baseSlug) {

        // Arrange
        EmpresaRepository mockRepo = mock(EmpresaRepository.class);
        CertificacionRepository mockCertRepo = mock(CertificacionRepository.class);
        SlugResolverService mockSlugResolver = mock(SlugResolverService.class);
        PerfilPublicoConsultaService svc = new PerfilPublicoConsultaService(mockRepo, mockCertRepo, mock(InsigniaEmpresaRepository.class), mock(ImaSnapshotRepository.class), mockSlugResolver);

        String normalizedSlug = baseSlug.toLowerCase();
        Empresa empresa = buildEmpresaActiva(normalizedSlug, "Empresa Test", "Plata");

        // Generate a random capitalization variant
        String capitalizedSlug = randomizeCapitalization(baseSlug);

        when(mockSlugResolver.resolver(capitalizedSlug)).thenReturn(empresa);
        when(mockCertRepo
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        PerfilPublicoResponseDTO dto = svc.obtenerPorSlug(capitalizedSlug);

        // Assert — same DTO regardless of capitalization
        assertThat(dto.getNombreEmpresa()).isEqualTo("Empresa Test");
        assertThat(dto.getNivelEcologico()).isEqualTo("Plata");
        assertThat(dto.getSectorIndustrial()).isEqualTo("MANUFACTURA");
        assertThat(dto.getPais()).isEqualTo("Costa Rica");
    }

    @Provide
    Arbitrary<String> slugValidoParaCapitalizacion() {
        // Generate slugs with lowercase letters, digits, and hyphens (1-20 chars)
        // that are valid after lowercasing
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> s.matches("^[a-z0-9-]{1,20}$"));
    }

    // --- Task 2.4: Property 5 — Rechazo de slug inválido ---

    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 5: Rechazo de slug inválido")
    void slugInvalido_lanzaPerfilNoEncontradoException(
            @ForAll("slugInvalido") String invalidSlug) {

        // Arrange
        EmpresaRepository mockRepo = mock(EmpresaRepository.class);
        CertificacionRepository mockCertRepo = mock(CertificacionRepository.class);
        SlugResolverService mockSlugResolver = mock(SlugResolverService.class);
        PerfilPublicoConsultaService svc = new PerfilPublicoConsultaService(mockRepo, mockCertRepo, mock(InsigniaEmpresaRepository.class), mock(ImaSnapshotRepository.class), mockSlugResolver);

        when(mockSlugResolver.resolver(invalidSlug))
                .thenThrow(new PerfilNoEncontradoException("El perfil que buscas no existe o ya no está disponible."));

        // Act & Assert
        assertThatThrownBy(() -> svc.obtenerPorSlug(invalidSlug))
                .isInstanceOf(PerfilNoEncontradoException.class)
                .hasMessage("El perfil que buscas no existe o ya no está disponible.");

        // Verify repository was never called
        verify(mockRepo, never()).findBySlugAndEstado(any(), any());
    }

    @Provide
    Arbitrary<String> slugInvalido() {
        Arbitrary<String> withSpecialChars = Arbitraries.strings()
                .withChars('@', '#', '$', '%', '&', '*', '!', '?', '+', '=', ' ', '/', '\\', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(50);

        Arbitrary<String> tooLong = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .withChars('-')
                .ofMinLength(121)
                .ofMaxLength(200);

        Arbitrary<String> empty = Arbitraries.of("", null);

        return Arbitraries.oneOf(withSpecialChars, tooLong, empty);
    }

    // --- Task 2.5: Property 3 — Conteo correcto de certificaciones e insignias ---

    @Property(tries = 100)
    @Tag("Feature: perfil-publico-reputacion, Property 3: Conteo correcto de certificaciones e insignias")
    void conteoCorrecto_certificacionesVigentes(
            @ForAll("slugValido") String slug,
            @ForAll @IntRange(min = 0, max = 10) int certVigentes,
            @ForAll @IntRange(min = 0, max = 10) int certVencidas) {

        // Arrange
        EmpresaRepository mockRepo = mock(EmpresaRepository.class);
        CertificacionRepository mockCertRepo = mock(CertificacionRepository.class);
        SlugResolverService mockSlugResolver = mock(SlugResolverService.class);
        PerfilPublicoConsultaService svc = new PerfilPublicoConsultaService(mockRepo, mockCertRepo, mock(InsigniaEmpresaRepository.class), mock(ImaSnapshotRepository.class), mockSlugResolver);

        Empresa empresa = buildEmpresaActiva(slug, "Empresa Certs", "Bronce");

        // Build list of vigentes (future expiry) — only these should be returned by repo
        List<Certificacion> vigentes = IntStream.range(0, certVigentes)
                .mapToObj(i -> buildCertificacion(empresa, LocalDate.now().plusDays(i + 1)))
                .collect(Collectors.toList());

        when(mockSlugResolver.resolver(slug)).thenReturn(empresa);
        // The repository method already filters by estado=ACTIVA and fechaVencimiento > now
        // So it only returns vigentes
        when(mockCertRepo
                .findByEmpresaIdAndEstadoAndFechaVencimientoGreaterThanOrderByFechaEmisionDesc(
                        eq(empresa.getId()), eq(EstadoCertificacion.ACTIVA), any(LocalDate.class)))
                .thenReturn(vigentes);

        // Act
        PerfilPublicoResponseDTO dto = svc.obtenerPorSlug(slug);

        // Assert
        assertThat(dto.getCertificacionesVigentes()).isEqualTo(certVigentes);
        // Insignias always 0 since PP-60 not yet implemented
        assertThat(dto.getInsigniasActivas()).isEqualTo(0);
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

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Empresa buildEmpresaActiva(String slug, String nombre, String nivelEcologico) {
        return Empresa.builder()
                .id(UUID.randomUUID())
                .slug(slug)
                .nombreEmpresa(nombre)
                .cedulaJuridica("3101000001")
                .sectorIndustrial(SectorIndustrial.MANUFACTURA)
                .pais("Costa Rica")
                .cantidadEmpleados(50)
                .correoCorporativo("info@test.com")
                .logoUrl("https://cdn.example.com/logo.png")
                .nivelEcologico(nivelEcologico)
                .estado(EstadoEmpresa.ACTIVO)
                .fechaRegistro(Instant.now())
                .build();
    }

    private Certificacion buildCertificacion(Empresa empresa, LocalDate fechaVencimiento) {
        return Certificacion.builder()
                .id(UUID.randomUUID())
                .idAuditoria(UUID.randomUUID())
                .empresa(empresa)
                .estado(EstadoCertificacion.ACTIVA)
                .fechaVencimiento(fechaVencimiento)
                .fechaEmision(Instant.now())
                .credencialJwt("dummy.jwt.token")
                .indiceEstado(new Random().nextLong())
                .build();
    }

    private String randomizeCapitalization(String input) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c) && random.nextBoolean()) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
