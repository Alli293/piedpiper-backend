package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.BenchmarkDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.CertificacionActivaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IMADTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.IndicadorAmbientalDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.PuntuacionAmbientalResponseDTO;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Componente responsable de calcular la Puntuación Ambiental de un establecimiento
 * a partir de sus indicadores ambientales (certificaciones, IMA y benchmarking).
 *
 * Fórmula: PuntuacionAmbiental = W_cert * scoreCertificaciones + W_ima * scoreIma + W_bench * scoreBenchmark
 *
 * Rango resultante: [0, 100].
 */
@Component
public class PuntuacionAmbientalCalculator {

    private static final BigDecimal W_CERT = new BigDecimal("0.50");
    private static final BigDecimal W_IMA = new BigDecimal("0.30");
    private static final BigDecimal W_BENCH = new BigDecimal("0.20");
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal BONUS_FECHA_RECIENTE = new BigDecimal("5");
    private static final BigDecimal SCORE_ESTIMADO_BASE = new BigDecimal("50");
    /** Ventana de recencia: 180 días (~6 meses). Instant.minus no acepta ChronoUnit.MONTHS. */
    private static final long DIAS_RECIENTE = 180;

    /**
     * Calcula la puntuación ambiental a partir de los indicadores disponibles.
     *
     * @param indicador Indicador ambiental con certificaciones activas, o null si no disponible
     * @param ima       IMA del establecimiento, o null si no disponible
     * @param benchmark Resultado de benchmarking, o null si no disponible
     * @return DTO con la puntuación total y sus componentes individuales, o null si no hay datos
     */
    public PuntuacionAmbientalResponseDTO calcular(
            @Nullable IndicadorAmbientalDTO indicador,
            @Nullable IMADTO ima,
            @Nullable BenchmarkDTO benchmark) {
        return calcular(indicador, ima, benchmark, null);
    }

    /**
     * Calcula la puntuación ambiental. Si no hay indicadores verificados pero hay un score
     * estimado por la IA, usa ese como fallback marcándolo como estimado.
     * Si no hay NADA (ni datos ni estimación), retorna null para no mostrar badge.
     */
    public PuntuacionAmbientalResponseDTO calcular(
            @Nullable IndicadorAmbientalDTO indicador,
            @Nullable IMADTO ima,
            @Nullable BenchmarkDTO benchmark,
            @Nullable Integer scoreEstimadoIA) {

        if (indicador == null && ima == null && benchmark == null) {
            if (scoreEstimadoIA == null) {
                // Sin ningún dato: no mostrar badge
                return null;
            }
            // Sin datos verificados pero con estimación de la IA
            PuntuacionAmbientalResponseDTO estimado = new PuntuacionAmbientalResponseDTO(
                    BigDecimal.valueOf(scoreEstimadoIA), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
            estimado.setEstimado(true);
            return estimado;
        }

        BigDecimal scoreCert = calcularScoreCertificaciones(indicador);
        BigDecimal scoreIma = calcularScoreIma(ima);
        BigDecimal scoreBench = calcularScoreBenchmark(benchmark);

        BigDecimal compCert = W_CERT.multiply(scoreCert);
        BigDecimal compIma = W_IMA.multiply(scoreIma);
        BigDecimal compBench = W_BENCH.multiply(scoreBench);

        BigDecimal total = compCert.add(compIma).add(compBench);

        int cantidadCerts = indicador != null && indicador.getCertificacionesActivas() != null
                ? indicador.getCertificacionesActivas().size() : 0;

        return new PuntuacionAmbientalResponseDTO(total, compCert, compIma, compBench, cantidadCerts);
    }

    /**
     * Calcula el score de certificaciones: min(cantidad * 20, 100) + bonus si la más reciente
     * fue emitida en los últimos 180 días. El resultado se acota a máximo 100.
     */
    BigDecimal calcularScoreCertificaciones(@Nullable IndicadorAmbientalDTO indicador) {
        if (indicador == null
                || indicador.getCertificacionesActivas() == null
                || indicador.getCertificacionesActivas().isEmpty()) {
            return BigDecimal.ZERO;
        }

        int cantidad = indicador.getCertificacionesActivas().size();
        BigDecimal base = BigDecimal.valueOf(Math.min(cantidad * 20L, 100));

        Instant fechaMasReciente = indicador.getCertificacionesActivas().stream()
                .map(CertificacionActivaDTO::getFechaEmision)
                .filter(f -> f != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (fechaMasReciente != null && fechaMasReciente.isAfter(Instant.now().minus(DIAS_RECIENTE, ChronoUnit.DAYS))) {
            base = base.add(BONUS_FECHA_RECIENTE).min(CIEN);
        }

        return base;
    }

    /**
     * Calcula el score IMA: el valor ya está normalizado 0–100 en el módulo IMA.
     * Se acota al rango [0, 100] como medida defensiva.
     */
    BigDecimal calcularScoreIma(@Nullable IMADTO ima) {
        if (ima == null || ima.getValorIma() == null) {
            return BigDecimal.ZERO;
        }
        return ima.getValorIma().min(CIEN).max(BigDecimal.ZERO);
    }

    /**
     * Calcula el score de benchmark según la posición:
     * LIDER=100, ARRIBA_PROMEDIO=75, PROMEDIO=50, DEBAJO_PROMEDIO=25.
     */
    BigDecimal calcularScoreBenchmark(@Nullable BenchmarkDTO benchmark) {
        if (benchmark == null || benchmark.getPosicion() == null) {
            return BigDecimal.ZERO;
        }
        return switch (benchmark.getPosicion()) {
            case LIDER -> CIEN;
            case ARRIBA_PROMEDIO -> BigDecimal.valueOf(75);
            case PROMEDIO -> BigDecimal.valueOf(50);
            case DEBAJO_PROMEDIO -> BigDecimal.valueOf(25);
        };
    }
}
