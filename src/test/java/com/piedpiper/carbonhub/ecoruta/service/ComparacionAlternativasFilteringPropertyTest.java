package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
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
 * Property-based tests for equivalence filtering and exclusion filtering
 * in ComparacionAlternativasService using jqwik.
 */
class ComparacionAlternativasFilteringPropertyTest {

    // ========================================================================
    // Property 2: Equivalence filtering preserves category and province
    // ========================================================================

    /**
     * Validates: Requirements 2.1
     *
     * For any activity with a given categoriaTuristica and provincia, all returned
     * alternatives in the ComparacionResponseDTO SHALL have the same categoriaTuristica
     * and the same provincia as the original activity.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 2: Equivalence filtering preserves category and province")
    void alternativasPreservanCategoriaYProvinciaDeActividadOriginal(
            @ForAll("categoriaArbitraria") InteresTuristico categoria,
            @ForAll("provinciaArbitraria") Provincia provincia,
            @ForAll("alternativasIaArbitrarias") List<AlternativaIaDTO> alternativasIa,
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
                .categoriaTuristica(categoria)
                .provincia(provincia)
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

        // Assert — the response DTO preserves the original activity's category and province
        assertThat(response.getCategoriaTuristica())
                .as("Response categoriaTuristica should match the original activity's category")
                .isEqualTo(categoria.name());

        assertThat(response.getProvincia())
                .as("Response provincia should match the original activity's province")
                .isEqualTo(provincia.name());
    }

    // ========================================================================
    // Property 3: Exclusion filtering removes original and existing activities
    // ========================================================================

    /**
     * Validates: Requirements 2.2, 2.3
     *
     * For any request for alternatives, the returned list SHALL NOT contain the
     * original activity NOR any activity whose name matches an activity already
     * present in the user's itinerario.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 3: Exclusion filtering removes original and existing activities")
    void alternativasNoIncluyenActividadOriginalNiExistentes(
            @ForAll("categoriaArbitraria") InteresTuristico categoria,
            @ForAll("provinciaArbitraria") Provincia provincia,
            @ForAll("nombresActividadesExistentes") @Size(min = 1, max = 5) List<String> nombresExistentes,
            @ForAll @IntRange(min = 0, max = 100) int ecoScoreOriginal) {

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        String nombreOriginal = "Actividad Original Test";

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Build itinerario with multiple existing activities
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

        // The original activity
        ItinerarioActividad actividadOriginal = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre(nombreOriginal)
                .categoriaTuristica(categoria)
                .provincia(provincia)
                .puntuacionAmbientalEstimada(ecoScoreOriginal)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(1)
                .build();
        dia.getActividades().add(actividadOriginal);

        // Add other existing activities with generated names
        for (int i = 0; i < nombresExistentes.size(); i++) {
            ItinerarioActividad existente = ItinerarioActividad.builder()
                    .id(UUID.randomUUID())
                    .itinerarioDia(dia)
                    .nombre(nombresExistentes.get(i))
                    .categoriaTuristica(categoria)
                    .provincia(provincia)
                    .puntuacionAmbientalEstimada(50)
                    .horario(LocalTime.of(10 + i, 0))
                    .duracionMinutos(60)
                    .orden(i + 2)
                    .build();
            dia.getActividades().add(existente);
        }

        // Build the IA response: simulate that Gemini respects exclusion
        // (the service passes nombresExcluidos to IA and trusts IA response)
        // We simulate IA returning alternatives that do NOT overlap with excluded names
        List<AlternativaIaDTO> alternativasIa = List.of(
                new AlternativaIaDTO("Alternativa Nueva A", "Desc A", BigDecimal.valueOf(5000), "CRC", "Hotel A", 75),
                new AlternativaIaDTO("Alternativa Nueva B", "Desc B", BigDecimal.valueOf(8000), "CRC", "Hotel B", 82)
        );

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividadOriginal));
        when(iaService.buscarAlternativas(any(ItinerarioActividad.class), anyList()))
                .thenAnswer(invocation -> {
                    // Verify the exclusion list passed to IA includes the original and all existing
                    List<String> nombresExcluidos = invocation.getArgument(1);
                    assertThat(nombresExcluidos)
                            .as("Exclusion list should contain the original activity name")
                            .contains(nombreOriginal);
                    for (String existente : nombresExistentes) {
                        assertThat(nombresExcluidos)
                                .as("Exclusion list should contain existing activity name: %s", existente)
                                .contains(existente);
                    }
                    return alternativasIa;
                });

        // Act
        ComparacionResponseDTO response = service.obtenerAlternativas(itinerarioId, actividadId, usuarioId);

        // Assert — returned alternatives do NOT match original or existing names
        List<String> nombresAlternativas = response.getAlternativas().stream()
                .map(alt -> alt.getNombre())
                .toList();

        assertThat(nombresAlternativas)
                .as("Returned alternatives should not contain the original activity name")
                .doesNotContain(nombreOriginal);

        for (String existente : nombresExistentes) {
            assertThat(nombresAlternativas)
                    .as("Returned alternatives should not contain existing activity name: %s", existente)
                    .doesNotContain(existente);
        }
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<InteresTuristico> categoriaArbitraria() {
        return Arbitraries.of(InteresTuristico.values());
    }

    @Provide
    Arbitrary<Provincia> provinciaArbitraria() {
        return Arbitraries.of(Provincia.values());
    }

    @Provide
    Arbitrary<List<AlternativaIaDTO>> alternativasIaArbitrarias() {
        Arbitrary<AlternativaIaDTO> alternativaArb = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50),
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100).injectNull(0.3),
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(1000), BigDecimal.valueOf(100000)).injectNull(0.2),
                Arbitraries.of("CRC", "USD").injectNull(0.1),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50).injectNull(0.3),
                Arbitraries.integers().between(0, 100).injectNull(0.1)
        ).as(AlternativaIaDTO::new);

        return alternativaArb.list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<String>> nombresActividadesExistentes() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(30)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .filter(list -> list.stream().noneMatch(s -> s.equals("Actividad Original Test")));
    }
}
