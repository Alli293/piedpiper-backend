package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.CertificacionActivaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PosicionBenchmark;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios y de propiedad para PuntuacionAmbientalCalculator.
 * 
 * Validates: Requirements 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 2.5
 */
class PuntuacionAmbientalCalculatorTest {

    private final PuntuacionAmbientalCalculator calculator = new PuntuacionAmbientalCalculator();

    // ========================================================================
    // Property Tests (jqwik)
    // ========================================================================

    /**
     * Property 1: Monotonía de puntuación ambiental.
     * Si A tiene más certificaciones que B (ceteris paribus), entonces Puntuacion(A) >= Puntuacion(B).
     *
     * Validates: Requirements 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 1: Monotonía de puntuación ambiental")
    void monotoniaMasCertificacionesImplicaMayorPuntuacion(
            @ForAll("certificacionesCountPair") int[] certCounts,
            @ForAll("imaValor") BigDecimal imaValor,
            @ForAll("posicionBenchmark") PosicionBenchmark posicion) {

        int countA = Math.max(certCounts[0], certCounts[1]);
        int countB = Math.min(certCounts[0], certCounts[1]);

        IndicadorAmbientalDTO indicadorA = crearIndicadorConCerts(countA);
        IndicadorAmbientalDTO indicadorB = crearIndicadorConCerts(countB);
        IMADTO ima = new IMADTO(UUID.randomUUID(), imaValor, false, Instant.now());
        BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), posicion, BigDecimal.TEN, BigDecimal.TEN, Instant.now());

        PuntuacionAmbientalResponseDTO puntuacionA = calculator.calcular(indicadorA, ima, benchmark);
        PuntuacionAmbientalResponseDTO puntuacionB = calculator.calcular(indicadorB, ima, benchmark);

        assertThat(puntuacionA.getPuntuacionTotal())
                .isGreaterThanOrEqualTo(puntuacionB.getPuntuacionTotal());
    }

    /**
     * Property 1 (IMA): Si A tiene mejor IMA que B (ceteris paribus), Puntuacion(A) >= Puntuacion(B).
     *
     * Validates: Requirements 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 1: Monotonía de puntuación ambiental")
    void monotoniaMejorImaImplicaMayorPuntuacion(
            @ForAll("imaValorPair") BigDecimal[] imaValues,
            @ForAll("posicionBenchmark") PosicionBenchmark posicion) {

        BigDecimal imaAlto = imaValues[0].max(imaValues[1]);
        BigDecimal imaBajo = imaValues[0].min(imaValues[1]);

        IMADTO imaA = new IMADTO(UUID.randomUUID(), imaAlto, false, Instant.now());
        IMADTO imaB = new IMADTO(UUID.randomUUID(), imaBajo, false, Instant.now());
        BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), posicion, BigDecimal.TEN, BigDecimal.TEN, Instant.now());

        PuntuacionAmbientalResponseDTO puntuacionA = calculator.calcular(null, imaA, benchmark);
        PuntuacionAmbientalResponseDTO puntuacionB = calculator.calcular(null, imaB, benchmark);

        assertThat(puntuacionA.getPuntuacionTotal())
                .isGreaterThanOrEqualTo(puntuacionB.getPuntuacionTotal());
    }

    /**
     * Property 1 (Benchmark): mejor posición de benchmark → mayor o igual puntuación.
     *
     * Validates: Requirements 2.2, 2.3, 2.4, 2.5
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 1: Monotonía de puntuación ambiental")
    void monotoniaMejorBenchmarkImplicaMayorPuntuacion(
            @ForAll("posicionBenchmarkPairOrdenada") PosicionBenchmark[] posiciones,
            @ForAll("imaValor") BigDecimal imaValor) {

        PosicionBenchmark mejorPosicion = posiciones[0];
        PosicionBenchmark peorPosicion = posiciones[1];

        IMADTO ima = new IMADTO(UUID.randomUUID(), imaValor, false, Instant.now());
        BenchmarkDTO benchmarkA = new BenchmarkDTO(UUID.randomUUID(), mejorPosicion, BigDecimal.TEN, BigDecimal.TEN, Instant.now());
        BenchmarkDTO benchmarkB = new BenchmarkDTO(UUID.randomUUID(), peorPosicion, BigDecimal.TEN, BigDecimal.TEN, Instant.now());

        PuntuacionAmbientalResponseDTO puntuacionA = calculator.calcular(null, ima, benchmarkA);
        PuntuacionAmbientalResponseDTO puntuacionB = calculator.calcular(null, ima, benchmarkB);

        assertThat(puntuacionA.getPuntuacionTotal())
                .isGreaterThanOrEqualTo(puntuacionB.getPuntuacionTotal());
    }

    /**
     * Property 4: Ordenamiento correcto por certificaciones.
     * Más certificaciones implica mayor scoreCertificaciones.
     * En empate, gana la fecha más reciente.
     *
     * Validates: Requirements 1.2, 1.3
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 4: Ordenamiento correcto por certificaciones")
    void masCertificacionesMayorScoreCertificaciones(
            @ForAll("certificacionesCountPair") int[] certCounts) {

        int countA = Math.max(certCounts[0], certCounts[1]);
        int countB = Math.min(certCounts[0], certCounts[1]);

        IndicadorAmbientalDTO indicadorA = crearIndicadorConCerts(countA);
        IndicadorAmbientalDTO indicadorB = crearIndicadorConCerts(countB);

        PuntuacionAmbientalResponseDTO puntuacionA = calculator.calcular(indicadorA, null, null);
        PuntuacionAmbientalResponseDTO puntuacionB = calculator.calcular(indicadorB, null, null);

        assertThat(puntuacionA.getComponenteCertificaciones())
                .isGreaterThanOrEqualTo(puntuacionB.getComponenteCertificaciones());
    }

    /**
     * Property 4 (tiebreaker): En empate de cantidad, la fecha más reciente obtiene mayor score.
     *
     * Validates: Requirements 1.2, 1.3
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 4: Ordenamiento correcto por certificaciones")
    void empateCertificacionesFechaRecienteGana(
            @ForAll @IntRange(min = 1, max = 4) int cantidadCerts) {

        // A tiene certificación reciente (dentro de 6 meses)
        IndicadorAmbientalDTO indicadorA = crearIndicadorConCertsYFecha(cantidadCerts, Instant.now().minus(30, ChronoUnit.DAYS));
        // B tiene certificación antigua (más de 6 meses)
        IndicadorAmbientalDTO indicadorB = crearIndicadorConCertsYFecha(cantidadCerts, Instant.now().minus(365, ChronoUnit.DAYS));

        PuntuacionAmbientalResponseDTO puntuacionA = calculator.calcular(indicadorA, null, null);
        PuntuacionAmbientalResponseDTO puntuacionB = calculator.calcular(indicadorB, null, null);

        assertThat(puntuacionA.getComponenteCertificaciones())
                .isGreaterThanOrEqualTo(puntuacionB.getComponenteCertificaciones());
    }

    /**
     * Property 8: Cálculo condicional de puntuación.
     * Al menos un indicador → puntuación > 0; ningún indicador → puntuación = 0.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 8: Cálculo condicional de puntuación")
    void alMenosUnIndicadorPuntuacionPositiva(
            @ForAll("indicadorOpcional") IndicadorAmbientalDTO indicador,
            @ForAll("imaOpcional") IMADTO ima,
            @ForAll("benchmarkOpcional") BenchmarkDTO benchmark) {

        boolean tieneAlMenosUno = indicador != null || ima != null || benchmark != null;

        PuntuacionAmbientalResponseDTO resultado = calculator.calcular(indicador, ima, benchmark);

        if (tieneAlMenosUno) {
            assertThat(resultado).isNotNull();
            assertThat(resultado.getPuntuacionTotal()).isGreaterThan(BigDecimal.ZERO);
            assertThat(resultado.isEstimado()).isFalse();
        } else {
            // Sin indicadores ni estimación → null
            assertThat(resultado).isNull();
        }
    }

    /**
     * Property 8: Sin ningún indicador ni estimación de IA, retorna null.
     *
     * Validates: Requirements 2.1
     */
    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 8: Cálculo condicional de puntuación")
    void sinIndicadoresPuntuacionEstimada() {
        PuntuacionAmbientalResponseDTO resultado = calculator.calcular(null, null, null);

        assertThat(resultado).isNull();
    }

    // ========================================================================
    // Property 7: Validación de indicadores (calculator processes what it receives)
    // The calculator itself does not filter by empresaId or date - it processes
    // whatever data is passed to it. The service layer is responsible for filtering.
    // This test verifies the calculator produces a deterministic, correct result
    // for any input combination it receives.
    //
    // Validates: Requirements 1.4, 2.6
    // ========================================================================

    @Property(tries = 100)
    @Tag("Feature: priorizacion-ambiental, Property 7: Validación de indicadores")
    void calculatorProcesaDeterministicamenteCualquierEntrada(
            @ForAll("indicadorOpcional") IndicadorAmbientalDTO indicador,
            @ForAll("imaOpcional") IMADTO ima,
            @ForAll("benchmarkOpcional") BenchmarkDTO benchmark) {

        PuntuacionAmbientalResponseDTO r1 = calculator.calcular(indicador, ima, benchmark);
        PuntuacionAmbientalResponseDTO r2 = calculator.calcular(indicador, ima, benchmark);

        // Deterministic: same input → same output
        assertThat(r1.getPuntuacionTotal()).isEqualByComparingTo(r2.getPuntuacionTotal());
        assertThat(r1.getComponenteCertificaciones()).isEqualByComparingTo(r2.getComponenteCertificaciones());
        assertThat(r1.getComponenteIma()).isEqualByComparingTo(r2.getComponenteIma());
        assertThat(r1.getComponenteBenchmark()).isEqualByComparingTo(r2.getComponenteBenchmark());

        // Total = sum of components
        BigDecimal expectedTotal = r1.getComponenteCertificaciones()
                .add(r1.getComponenteIma())
                .add(r1.getComponenteBenchmark());
        assertThat(r1.getPuntuacionTotal()).isEqualByComparingTo(expectedTotal);
    }

    // ========================================================================
    // Providers for jqwik
    // ========================================================================

    @Provide
    Arbitrary<int[]> certificacionesCountPair() {
        return Arbitraries.integers().between(0, 10)
                .tuple2()
                .map(t -> new int[]{t.get1(), t.get2()});
    }

    @Provide
    Arbitrary<BigDecimal> imaValor() {
        return Arbitraries.integers().between(0, 100)
                .map(BigDecimal::valueOf);
    }

    @Provide
    Arbitrary<BigDecimal[]> imaValorPair() {
        return Arbitraries.integers().between(0, 100)
                .tuple2()
                .map(t -> new BigDecimal[]{BigDecimal.valueOf(t.get1()), BigDecimal.valueOf(t.get2())});
    }

    @Provide
    Arbitrary<PosicionBenchmark> posicionBenchmark() {
        return Arbitraries.of(PosicionBenchmark.values());
    }

    @Provide
    Arbitrary<PosicionBenchmark[]> posicionBenchmarkPairOrdenada() {
        return Arbitraries.of(PosicionBenchmark.values())
                .tuple2()
                .map(t -> {
                    PosicionBenchmark a = t.get1();
                    PosicionBenchmark b = t.get2();
                    // Order: LIDER > ARRIBA_PROMEDIO > PROMEDIO > DEBAJO_PROMEDIO
                    if (ordenPosicion(a) >= ordenPosicion(b)) {
                        return new PosicionBenchmark[]{a, b};
                    } else {
                        return new PosicionBenchmark[]{b, a};
                    }
                });
    }

    @Provide
    Arbitrary<IndicadorAmbientalDTO> indicadorOpcional() {
        Arbitrary<IndicadorAmbientalDTO> conDatos = Arbitraries.integers().between(1, 8)
                .map(this::crearIndicadorConCerts);
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(3, conDatos),
                net.jqwik.api.Tuple.of(1, Arbitraries.just(null))
        );
    }

    @Provide
    Arbitrary<IMADTO> imaOpcional() {
        Arbitrary<IMADTO> conDatos = Arbitraries.integers().between(1, 100)
                .map(v -> new IMADTO(UUID.randomUUID(), BigDecimal.valueOf(v), false, Instant.now()));
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(3, conDatos),
                net.jqwik.api.Tuple.of(1, Arbitraries.just(null))
        );
    }

    @Provide
    Arbitrary<BenchmarkDTO> benchmarkOpcional() {
        Arbitrary<BenchmarkDTO> conDatos = Arbitraries.of(PosicionBenchmark.values())
                .map(p -> new BenchmarkDTO(UUID.randomUUID(), p, BigDecimal.TEN, BigDecimal.TEN, Instant.now()));
        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(3, conDatos),
                net.jqwik.api.Tuple.of(1, Arbitraries.just(null))
        );
    }

    // ========================================================================
    // Unit Tests (Task 2.6)
    // ========================================================================

    @Nested
    @DisplayName("Unit Tests - PuntuacionAmbientalCalculator")
    class UnitTests {

        @Test
        @DisplayName("Solo certificaciones: 3 certs → score = 0.50 * min(3*20, 100) = 30")
        void soloCertificaciones() {
            IndicadorAmbientalDTO indicador = crearIndicadorConCertsYFecha(3, Instant.now().minus(365, ChronoUnit.DAYS));

            PuntuacionAmbientalResponseDTO result = calculator.calcular(indicador, null, null);

            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getComponenteIma()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPuntuacionTotal()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getCantidadCertificacionesActivas()).isEqualTo(3);
        }

        @Test
        @DisplayName("Solo IMA: valor 80 → score = 0.30 * 80 = 24")
        void soloIma() {
            IMADTO ima = new IMADTO(UUID.randomUUID(), new BigDecimal("80"), false, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, ima, null);

            assertThat(result.getComponenteIma()).isEqualByComparingTo(new BigDecimal("24.00"));
            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPuntuacionTotal()).isEqualByComparingTo(new BigDecimal("24.00"));
        }

        @Test
        @DisplayName("Solo benchmark LIDER: score = 0.20 * 100 = 20")
        void soloBenchmarkLider() {
            BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), PosicionBenchmark.LIDER,
                    BigDecimal.TEN, BigDecimal.TEN, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, null, benchmark);

            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getComponenteIma()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPuntuacionTotal()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("Solo benchmark DEBAJO_PROMEDIO: score = 0.20 * 25 = 5")
        void soloBenchmarkDebajo() {
            BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), PosicionBenchmark.DEBAJO_PROMEDIO,
                    BigDecimal.TEN, BigDecimal.TEN, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, null, benchmark);

            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(result.getPuntuacionTotal()).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Combinación completa: 5 certs recientes + IMA 60 + ARRIBA_PROMEDIO")
        void combinacionCompleta() {
            IndicadorAmbientalDTO indicador = crearIndicadorConCertsYFecha(5, Instant.now().minus(10, ChronoUnit.DAYS));
            IMADTO ima = new IMADTO(UUID.randomUUID(), new BigDecimal("60"), false, Instant.now());
            BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), PosicionBenchmark.ARRIBA_PROMEDIO,
                    BigDecimal.TEN, BigDecimal.TEN, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(indicador, ima, benchmark);

            // scoreCert = min(5*20, 100) + 5 (bonus reciente) = 100 + 5 → capped at 100
            // componenteCert = 0.50 * 100 = 50
            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(new BigDecimal("50.00"));
            // componenteIma = 0.30 * 60 = 18
            assertThat(result.getComponenteIma()).isEqualByComparingTo(new BigDecimal("18.00"));
            // componenteBenchmark = 0.20 * 75 = 15
            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(new BigDecimal("15.00"));
            // total = 50 + 18 + 15 = 83
            assertThat(result.getPuntuacionTotal()).isEqualByComparingTo(new BigDecimal("83.00"));
            assertThat(result.getCantidadCertificacionesActivas()).isEqualTo(5);
        }

        @Test
        @DisplayName("Todos null → retorna null (sin datos)")
        void todosNullRetornaNull() {
            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, null, null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Certificaciones con lista vacía → sin aporte de certificaciones")
        void certificacionesListaVacia() {
            IndicadorAmbientalDTO indicador = new IndicadorAmbientalDTO(UUID.randomUUID(), List.of(), Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(indicador, null, null);

            // Indicador exists but no certifications → it's still "at least one indicator"
            // but scoreCert = 0, so total depends on whether the calculator returns 0 for this case
            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("IMA valor negativo se acota a 0")
        void imaValorNegativoAcotado() {
            IMADTO ima = new IMADTO(UUID.randomUUID(), new BigDecimal("-10"), false, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, ima, null);

            assertThat(result.getComponenteIma()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("IMA valor superior a 100 se acota a 100")
        void imaValorSuperiorAcotado() {
            IMADTO ima = new IMADTO(UUID.randomUUID(), new BigDecimal("150"), false, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, ima, null);

            // 0.30 * 100 = 30
            assertThat(result.getComponenteIma()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("Benchmark null posición → score 0")
        void benchmarkNullPosicion() {
            BenchmarkDTO benchmark = new BenchmarkDTO(UUID.randomUUID(), null, BigDecimal.TEN, BigDecimal.TEN, Instant.now());

            PuntuacionAmbientalResponseDTO result = calculator.calcular(null, null, benchmark);

            assertThat(result.getComponenteBenchmark()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Bonus por fecha reciente: cert emitida hace 1 mes")
        void bonusFechaReciente() {
            // 2 certs, one recent → base = min(2*20,100)=40, +5 bonus = 45
            // componenteCert = 0.50 * 45 = 22.5
            IndicadorAmbientalDTO indicador = crearIndicadorConCertsYFecha(2, Instant.now().minus(15, ChronoUnit.DAYS));

            PuntuacionAmbientalResponseDTO result = calculator.calcular(indicador, null, null);

            assertThat(result.getComponenteCertificaciones()).isEqualByComparingTo(new BigDecimal("22.50"));
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private IndicadorAmbientalDTO crearIndicadorConCerts(int cantidad) {
        // Use old dates by default (no bonus)
        return crearIndicadorConCertsYFecha(cantidad, Instant.now().minus(365, ChronoUnit.DAYS));
    }

    private IndicadorAmbientalDTO crearIndicadorConCertsYFecha(int cantidad, Instant fechaMasReciente) {
        if (cantidad <= 0) {
            return new IndicadorAmbientalDTO(UUID.randomUUID(), List.of(), Instant.now());
        }
        List<CertificacionActivaDTO> certs = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Instant fecha = (i == 0) ? fechaMasReciente : Instant.now().minus(400, ChronoUnit.DAYS);
            certs.add(new CertificacionActivaDTO(UUID.randomUUID(), "Cert-" + i, "activa", fecha));
        }
        return new IndicadorAmbientalDTO(UUID.randomUUID(), certs, Instant.now());
    }

    private int ordenPosicion(PosicionBenchmark posicion) {
        return switch (posicion) {
            case LIDER -> 4;
            case ARRIBA_PROMEDIO -> 3;
            case PROMEDIO -> 2;
            case DEBAJO_PROMEDIO -> 1;
        };
    }
}
