package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.MetricasAuditor;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.TipoCertificacionSolicitud;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Property-based test for PerfilPublicoAuditorService.calcularMetricas.
 *
 * Validates: Requirements 1.3
 */
class PerfilPublicoAuditorMetricasPropertyTest {

    private final PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
    private final CertificacionRepository certificacionRepository = mock(CertificacionRepository.class);
    private final SolicitudAuditoriaRepository solicitudAuditoriaRepository = mock(SolicitudAuditoriaRepository.class);
    private final CatalogoTiposCertificacion catalogoTiposCertificacion = mock(CatalogoTiposCertificacion.class);
    private final PerfilPublicoAuditorMapper mapper = mock(PerfilPublicoAuditorMapper.class);
    private final Clock clock = Clock.fixed(
            ZonedDateTime.of(2025, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            ZoneId.of("UTC"));

    private final PerfilPublicoAuditorService service = new PerfilPublicoAuditorService(
            perfilAuditorRepository,
            certificacionRepository,
            solicitudAuditoriaRepository,
            catalogoTiposCertificacion,
            mapper,
            clock);

    /**
     * Property 1: Cálculo correcto de métricas desde registros históricos
     *
     * For any set of completed audit requests with ratings and dates, verify:
     * - calificacionPromedio == value from PerfilAuditor (passed through)
     * - totalResenas == value from PerfilAuditor (passed through)
     * - auditoriasCompletadas == size of the solicitudes list
     * - tiempoPromedioRespuestaDias == average of days between fechaAsignacion and fechaAceptacion (1 decimal)
     *
     * Validates: Requirements 1.3
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 1: Cálculo correcto de métricas desde registros históricos")
    void metricasCalculadasCorrectamente(
            @ForAll("solicitudesConFechas") List<SolicitudAuditoria> solicitudes,
            @ForAll("calificacionArbitraria") BigDecimal calificacionPromedio,
            @ForAll("totalResenasArbitrario") int totalResenas) {

        // Arrange: build a PerfilAuditor with the given calificacion and resenas
        PerfilAuditor perfil = PerfilAuditor.builder()
                .calificacionPromedio(calificacionPromedio)
                .totalResenas(totalResenas)
                .tiempoRespuestaHoras(48) // fallback value, should not be used when solicitudes have dates
                .build();

        // Act
        MetricasAuditor metricas = service.calcularMetricas(perfil, solicitudes);

        // Assert
        assertThat(metricas).isNotNull();

        // calificacionPromedio comes directly from perfil
        assertThat(metricas.calificacionPromedio())
                .as("calificacionPromedio should match the value from PerfilAuditor")
                .isEqualByComparingTo(calificacionPromedio);

        // totalResenas comes directly from perfil
        assertThat(metricas.totalResenas())
                .as("totalResenas should match the value from PerfilAuditor")
                .isEqualTo(totalResenas);

        // auditoriasCompletadas == size of the list
        assertThat(metricas.auditoriasCompletadas())
                .as("auditoriasCompletadas should equal the count of solicitudes")
                .isEqualTo(solicitudes.size());

        // tiempoPromedioRespuestaDias: compute expected value
        List<SolicitudAuditoria> conFechas = solicitudes.stream()
                .filter(s -> s.getFechaAsignacion() != null && s.getFechaAceptacion() != null)
                .collect(Collectors.toList());

        if (!conFechas.isEmpty()) {
            long totalHoras = conFechas.stream()
                    .mapToLong(s -> Duration.between(s.getFechaAsignacion(), s.getFechaAceptacion()).toHours())
                    .sum();
            BigDecimal expectedDias = BigDecimal.valueOf(totalHoras)
                    .divide(BigDecimal.valueOf((long) conFechas.size() * 24L), 1, RoundingMode.HALF_UP);

            assertThat(metricas.tiempoPromedioRespuestaDias())
                    .as("tiempoPromedioRespuestaDias should be the average days between fechaAsignacion and fechaAceptacion")
                    .isEqualByComparingTo(expectedDias);
        } else {
            // Fallback: uses perfil.getTiempoRespuestaHoras() / 24
            BigDecimal expectedFallback = BigDecimal.valueOf(48)
                    .divide(BigDecimal.valueOf(24), 1, RoundingMode.HALF_UP);
            assertThat(metricas.tiempoPromedioRespuestaDias())
                    .as("tiempoPromedioRespuestaDias should fallback to perfil.tiempoRespuestaHoras/24")
                    .isEqualByComparingTo(expectedFallback);
        }
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<List<SolicitudAuditoria>> solicitudesConFechas() {
        return solicitudArbitraria().list().ofMinSize(1).ofMaxSize(20);
    }

    private Arbitrary<SolicitudAuditoria> solicitudArbitraria() {
        // Generate a base instant between 2020-01-01 and 2025-01-01
        Arbitrary<Instant> baseInstant = Arbitraries.longs()
                .between(
                        Instant.parse("2020-01-01T00:00:00Z").getEpochSecond(),
                        Instant.parse("2025-01-01T00:00:00Z").getEpochSecond())
                .map(Instant::ofEpochSecond);

        // Generate a response time in hours (1 hour to 30 days = 720 hours)
        Arbitrary<Long> horasRespuesta = Arbitraries.longs().between(1L, 720L);

        // Whether this solicitud will have both dates or not
        Arbitrary<Boolean> tieneFechas = Arbitraries.of(true, true, true, false);

        Arbitrary<TipoCertificacionSolicitud> tipo = Arbitraries.of(TipoCertificacionSolicitud.values());

        return Combinators.combine(baseInstant, horasRespuesta, tieneFechas, tipo)
                .as((fechaAsignacion, horas, conFechas, tipoCert) -> {
                    SolicitudAuditoria.SolicitudAuditoriaBuilder builder = SolicitudAuditoria.builder()
                            .tipoCertificacion(tipoCert);

                    if (conFechas) {
                        Instant fechaAceptacion = fechaAsignacion.plus(Duration.ofHours(horas));
                        builder.fechaAsignacion(fechaAsignacion)
                                .fechaAceptacion(fechaAceptacion);
                    }
                    // If !conFechas, both fechaAsignacion and fechaAceptacion remain null

                    return builder.build();
                });
    }

    @Provide
    Arbitrary<BigDecimal> calificacionArbitraria() {
        // Ratings between 1.0 and 5.0 with 1 decimal place
        return Arbitraries.integers().between(10, 50)
                .map(i -> BigDecimal.valueOf(i).divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP));
    }

    @Provide
    Arbitrary<Integer> totalResenasArbitrario() {
        return Arbitraries.integers().between(0, 500);
    }
}
