package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.CertificacionPublicaDTO;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.config.DefinicionCertificacion;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoCertificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.TipoLogroOpenBadges;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;

import net.jqwik.api.*;
import net.jqwik.time.api.DateTimes;
import net.jqwik.time.api.Dates;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for certification vigencia calculation.
 * Validates: Requirements 4.1, 4.2, 4.3, 4.5
 *
 * Property 3: Cálculo correcto de vigencia de certificaciones
 * - fechaVencimiento < hoy → vencida=true
 * - fechaVencimiento >= hoy → vencida=false
 * - fechaVencimiento null → vencida=true
 */
class PerfilPublicoAuditorVigenciaPropertyTest {

    private static final LocalDate HOY = LocalDate.of(2025, 6, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            HOY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private final PerfilPublicoAuditorService service;

    PerfilPublicoAuditorVigenciaPropertyTest() {
        PerfilAuditorRepository perfilRepo = mock(PerfilAuditorRepository.class);
        CertificacionRepository certRepo = mock(CertificacionRepository.class);
        SolicitudAuditoriaRepository solicitudRepo = mock(SolicitudAuditoriaRepository.class);
        CalificacionRepository calificacionRepo = mock(CalificacionRepository.class);
        CatalogoTiposCertificacion catalogo = mock(CatalogoTiposCertificacion.class);
        PerfilPublicoAuditorMapper mapper = mock(PerfilPublicoAuditorMapper.class);

        // Mock catalogo to return a valid definition for any type
        DefinicionCertificacion defMock = new DefinicionCertificacion(
                TipoCertificacion.INVENTARIO_GEI,
                "Inventario de GEI",
                "Descripción",
                12,
                TipoLogroOpenBadges.CERTIFICATE,
                "Criterio"
        );
        when(catalogo.buscar(any())).thenReturn(Optional.of(defMock));

        this.service = new PerfilPublicoAuditorService(
                perfilRepo, certRepo, solicitudRepo, calificacionRepo, catalogo, mapper, FIXED_CLOCK
        );
    }

    /**
     * Validates: Requirements 4.1, 4.2, 4.3, 4.5
     *
     * For any certification with fechaVencimiento before today, the vencida flag
     * must be true. For fechaVencimiento equal to or after today, vencida must be
     * false. For null fechaVencimiento, vencida must be true.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 3: Cálculo correcto de vigencia de certificaciones")
    void vigencia_calculaCorrectamenteSegunFechaVencimiento(
            @ForAll("fechaVencimientoArbitraria") LocalDate fechaVencimiento,
            @ForAll("tipoCertificacionArbitrario") TipoCertificacion tipo) {

        // Arrange
        Certificacion cert = Certificacion.builder()
                .tipo(tipo)
                .fechaVencimiento(fechaVencimiento)
                .build();

        // Act
        List<CertificacionPublicaDTO> resultado = service.calcularCertificacionesPublicas(List.of(cert));

        // Assert
        assertThat(resultado).hasSize(1);
        CertificacionPublicaDTO dto = resultado.get(0);

        if (fechaVencimiento == null) {
            assertThat(dto.isVencida())
                    .as("fechaVencimiento null → vencida debe ser true")
                    .isTrue();
        } else if (fechaVencimiento.isBefore(HOY)) {
            assertThat(dto.isVencida())
                    .as("fechaVencimiento %s < hoy %s → vencida debe ser true", fechaVencimiento, HOY)
                    .isTrue();
        } else {
            assertThat(dto.isVencida())
                    .as("fechaVencimiento %s >= hoy %s → vencida debe ser false", fechaVencimiento, HOY)
                    .isFalse();
        }

        // Also verify fechaVigencia in the DTO matches the original
        assertThat(dto.getFechaVigencia()).isEqualTo(fechaVencimiento);
    }

    /**
     * Validates: Requirements 4.1, 4.2, 4.3, 4.5
     *
     * Boundary case: a certification expiring exactly today must NOT be considered expired.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 3: Cálculo correcto de vigencia de certificaciones")
    void vigencia_fechaIgualAHoy_noEstaVencida(
            @ForAll("tipoCertificacionArbitrario") TipoCertificacion tipo) {

        // Arrange — fechaVencimiento = hoy exactly
        Certificacion cert = Certificacion.builder()
                .tipo(tipo)
                .fechaVencimiento(HOY)
                .build();

        // Act
        List<CertificacionPublicaDTO> resultado = service.calcularCertificacionesPublicas(List.of(cert));

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isVencida())
                .as("fechaVencimiento == hoy → vencida debe ser false (vigente hasta fin del día)")
                .isFalse();
    }

    /**
     * Validates: Requirements 4.1, 4.2, 4.3, 4.5
     *
     * A null fechaVencimiento must always result in vencida=true.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 3: Cálculo correcto de vigencia de certificaciones")
    void vigencia_fechaNula_siempreVencida(
            @ForAll("tipoCertificacionArbitrario") TipoCertificacion tipo) {

        // Arrange
        Certificacion cert = Certificacion.builder()
                .tipo(tipo)
                .fechaVencimiento(null)
                .build();

        // Act
        List<CertificacionPublicaDTO> resultado = service.calcularCertificacionesPublicas(List.of(cert));

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isVencida())
                .as("fechaVencimiento null → vencida debe ser true")
                .isTrue();
        assertThat(resultado.get(0).getFechaVigencia())
                .as("fechaVigencia en el DTO debe ser null cuando fechaVencimiento es null")
                .isNull();
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<LocalDate> fechaVencimientoArbitraria() {
        // Generate dates around "hoy" (2025-06-15): past, today, and future
        // Also includes null to test the null case
        Arbitrary<LocalDate> pastDates = Dates.dates()
                .between(LocalDate.of(2020, 1, 1), HOY.minusDays(1));
        Arbitrary<LocalDate> futureDates = Dates.dates()
                .between(HOY, LocalDate.of(2030, 12, 31));
        Arbitrary<LocalDate> nullDate = Arbitraries.just(null);

        return Arbitraries.oneOf(pastDates, futureDates, nullDate);
    }

    @Provide
    Arbitrary<TipoCertificacion> tipoCertificacionArbitrario() {
        return Arbitraries.of(TipoCertificacion.values());
    }
}
