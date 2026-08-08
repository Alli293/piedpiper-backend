package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.AlternativaIaDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ComparacionResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioActividadRepository;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for difference calculation, sorting, and best marking
 * in ComparacionAlternativasService using jqwik.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 */
class ComparacionAlternativasDiffPropertyTest {

    private static final int ECO_SCORE_DEFAULT = 50;

    // ========================================================================
    // Property 4: Difference calculation is correct and list is sorted with best marked
    // ========================================================================

    /**
     * Validates: Requirements 3.1, 3.2, 3.3, 3.4
     *
     * For any list of alternatives with varying EcoScores and a given original EcoScore,
     * the diferenciaAmbiental of each alternative SHALL equal alternativa.ecoScore - ecoScoreOriginal,
     * the list SHALL be sorted by ecoScore descending, and exactly one alternative (the first)
     * SHALL have mejorDesempeno = true.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 4: Difference calculation is correct and list is sorted with best marked")
    void diferenciaAmbientalCorrectaListaOrdenadaYMejorMarcado(
            @ForAll("alternativasIaConEcoScore") @Size(min = 1, max = 10) List<AlternativaIaDTO> alternativasIa,
            @ForAll @IntRange(min = 0, max = 100) int ecoScoreOriginal) {

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Build itinerario with a single activity (the original)
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario)
                .dias(new ArrayList<>())
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .id(UUID.randomUUID())
                .itinerario(itinerario)
                .numeroDia(1)
                .fecha(LocalDate.now())
                .orden(1)
                .actividades(new ArrayList<>())
                .build();
        itinerario.getDias().add(dia);

        ItinerarioActividad actividadOriginal = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre("Actividad Original")
                .categoriaTuristica(InteresTuristico.NATURALEZA)
                .provincia(Provincia.SAN_JOSE)
                .puntuacionAmbientalEstimada(ecoScoreOriginal)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(1)
                .build();
        dia.getActividades().add(actividadOriginal);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividadOriginal));
        when(iaService.buscarAlternativas(any(ItinerarioActividad.class), anyList()))
                .thenReturn(alternativasIa);

        // Act
        ComparacionResponseDTO response = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        // Assert
        List<AlternativaDTO> alternativas = response.getAlternativas();
        assertThat(alternativas).isNotEmpty();

        // 1. Verify diferenciaAmbiental == alternativa.ecoScore - ecoScoreOriginal for ALL
        for (AlternativaDTO alt : alternativas) {
            int expectedEcoScore = alt.getEcoScore();
            int expectedDiferencia = expectedEcoScore - ecoScoreOriginal;
            assertThat(alt.getDiferenciaAmbiental())
                    .as("diferenciaAmbiental for '%s' should be ecoScore(%d) - ecoScoreOriginal(%d) = %d",
                            alt.getNombre(), expectedEcoScore, ecoScoreOriginal, expectedDiferencia)
                    .isEqualTo(expectedDiferencia);
        }

        // 2. Verify list is sorted by ecoScore descending
        for (int i = 0; i < alternativas.size() - 1; i++) {
            assertThat(alternativas.get(i).getEcoScore())
                    .as("Alternative at index %d should have ecoScore >= alternative at index %d", i, i + 1)
                    .isGreaterThanOrEqualTo(alternativas.get(i + 1).getEcoScore());
        }

        // 3. Verify exactly one alternative (the first) has mejorDesempeno = true
        assertThat(alternativas.get(0).isMejorDesempeno())
                .as("First alternative (highest ecoScore) should have mejorDesempeno = true")
                .isTrue();

        long countMejorDesempeno = alternativas.stream()
                .filter(AlternativaDTO::isMejorDesempeno)
                .count();
        assertThat(countMejorDesempeno)
                .as("Exactly one alternative should have mejorDesempeno = true")
                .isEqualTo(1);
    }

    /**
     * Validates: Requirements 3.1, 3.2, 3.3, 3.4
     *
     * When ecoScoreOriginal is null (defaults to 50), the diferenciaAmbiental
     * SHALL be calculated using the default value of 50.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 4: Difference calculation is correct and list is sorted with best marked")
    void diferenciaAmbientalUsaDefaultCuandoEcoScoreOriginalEsNull(
            @ForAll("alternativasIaConEcoScore") @Size(min = 1, max = 5) List<AlternativaIaDTO> alternativasIa) {

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Build itinerario with original activity that has null puntuacionAmbientalEstimada
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario)
                .dias(new ArrayList<>())
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .id(UUID.randomUUID())
                .itinerario(itinerario)
                .numeroDia(1)
                .fecha(LocalDate.now())
                .orden(1)
                .actividades(new ArrayList<>())
                .build();
        itinerario.getDias().add(dia);

        ItinerarioActividad actividadOriginal = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre("Actividad Original")
                .categoriaTuristica(InteresTuristico.NATURALEZA)
                .provincia(Provincia.SAN_JOSE)
                .puntuacionAmbientalEstimada(null) // null → defaults to 50
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(1)
                .build();
        dia.getActividades().add(actividadOriginal);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividadOriginal));
        when(iaService.buscarAlternativas(any(ItinerarioActividad.class), anyList()))
                .thenReturn(alternativasIa);

        // Act
        ComparacionResponseDTO response = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        // Assert — diferenciaAmbiental uses default 50 as ecoScoreOriginal
        List<AlternativaDTO> alternativas = response.getAlternativas();
        assertThat(alternativas).isNotEmpty();

        for (AlternativaDTO alt : alternativas) {
            int expectedDiferencia = alt.getEcoScore() - ECO_SCORE_DEFAULT;
            assertThat(alt.getDiferenciaAmbiental())
                    .as("diferenciaAmbiental should use default ecoScoreOriginal=50 when original is null")
                    .isEqualTo(expectedDiferencia);
        }

        // Also verify ecoScoreOriginal in response is 50
        assertThat(response.getEcoScoreOriginal())
                .as("Response ecoScoreOriginal should be default 50 when original has null score")
                .isEqualTo(ECO_SCORE_DEFAULT);
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<List<AlternativaIaDTO>> alternativasIaConEcoScore() {
        Arbitrary<AlternativaIaDTO> alternativaArb = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(80).injectNull(0.3),
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000)).injectNull(0.2),
                Arbitraries.of("CRC", "USD").injectNull(0.1),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30).injectNull(0.3),
                Arbitraries.integers().between(0, 100)
        ).as(AlternativaIaDTO::new);

        return alternativaArb.list().ofMinSize(1).ofMaxSize(10);
    }
}
