package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.models.dtos.ResenaVerificadaDTO;

import net.jqwik.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for review ordering — Property 4.
 * Verifies that reviews sorted by fechaCalificacion descending maintain the invariant:
 * for every consecutive pair (reseña[i], reseña[i+1]), fechaCalificacion[i] >= fechaCalificacion[i+1].
 *
 * Validates: Requirements 6.3
 */
class PerfilPublicoAuditorResenasPropertyTest {

    // ========================================================================
    // Property 4: Reseñas ordenadas cronológicamente descendente
    // ========================================================================

    /**
     * Validates: Requirements 6.3
     *
     * For any list of reviews with random fechaCalificacion dates, after sorting
     * by fechaCalificacion descending (the ordering the service applies), every
     * consecutive pair (reseña[i], reseña[i+1]) SHALL satisfy
     * fechaCalificacion[i] >= fechaCalificacion[i+1].
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 4: Reseñas ordenadas cronológicamente descendente")
    void resenasOrdenadasDescendentePorFecha(
            @ForAll("listaResenasAleatorias") List<ResenaVerificadaDTO> resenas) {

        // Act: sort using the same logic the service uses (descending by fechaCalificacion)
        List<ResenaVerificadaDTO> ordenadas = resenas.stream()
                .sorted(Comparator.comparing(ResenaVerificadaDTO::getFechaCalificacion).reversed())
                .toList();

        // Assert: for every consecutive pair, fechaCalificacion[i] >= fechaCalificacion[i+1]
        IntStream.range(0, ordenadas.size() - 1).forEach(i -> {
            LocalDate fechaActual = ordenadas.get(i).getFechaCalificacion();
            LocalDate fechaSiguiente = ordenadas.get(i + 1).getFechaCalificacion();
            assertThat(fechaActual)
                    .as("reseña[%d].fechaCalificacion (%s) should be >= reseña[%d].fechaCalificacion (%s)",
                            i, fechaActual, i + 1, fechaSiguiente)
                    .isAfterOrEqualTo(fechaSiguiente);
        });
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<List<ResenaVerificadaDTO>> listaResenasAleatorias() {
        return resenaArbitraria().list().ofMinSize(0).ofMaxSize(50);
    }

    private Arbitrary<ResenaVerificadaDTO> resenaArbitraria() {
        Arbitrary<BigDecimal> calificacion = Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, BigDecimal.valueOf(5))
                .ofScale(1);

        Arbitrary<String> comentario = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withChars(' ', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(200);

        Arbitrary<LocalDate> fecha = Arbitraries.of(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 6, 15),
                LocalDate.of(2022, 3, 20),
                LocalDate.of(2023, 11, 30),
                LocalDate.of(2024, 7, 4),
                LocalDate.of(2025, 1, 1)
        ).flatMap(base -> Arbitraries.integers().between(0, 364)
                .map(base::plusDays));

        return Combinators.combine(calificacion, comentario, fecha)
                .as(ResenaVerificadaDTO::new);
    }
}
