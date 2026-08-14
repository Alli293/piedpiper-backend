package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;
import com.piedpiper.carbonhub.ecoruta.models.enums.Provincia;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioActividadRepository;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import net.jqwik.api.*;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for substitution logic in ComparacionAlternativasService using jqwik.
 *
 * Property 5: Substitution preserves structural position and updates score
 * Property 6: Substitution rejects non-equivalent alternatives
 */
class ComparacionAlternativasSustitucionPropertyTest {

    // ========================================================================
    // Property 5: Substitution preserves structural position and updates score
    // ========================================================================

    /**
     * Validates: Requirements 4.1, 4.3
     *
     * For any valid substitution, the resulting activity SHALL occupy the same orden
     * and belong to the same ItinerarioDia as the original, and its puntuacionAmbientalEstimada
     * SHALL equal the alternative's ecoScore.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 5: Substitution preserves structural position and updates score")
    void sustitucionPreservaPosicionYActualizaPuntuacion(
            @ForAll("categoriaArbitraria") InteresTuristico categoria,
            @ForAll("provinciaArbitraria") Provincia provincia,
            @ForAll("ordenArbitrario") int orden,
            @ForAll("numeroDiaArbitrario") int numeroDia,
            @ForAll("ecoScoreArbitrario") int ecoScoreOriginal,
            @ForAll("ecoScoreArbitrario") int ecoScoreAlternativa,
            @ForAll("nombreArbitrario") String nombreAlternativa) {

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID diaId = UUID.randomUUID();

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper, mock(jakarta.persistence.EntityManager.class));

        // Build itinerario structure
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        Itinerario itinerario = Itinerario.builder()
                .id(itinerarioId)
                .usuario(usuario)
                .dias(new ArrayList<>())
                .build();

        ItinerarioDia dia = ItinerarioDia.builder()
                .id(diaId)
                .itinerario(itinerario)
                .numeroDia(numeroDia)
                .fecha(LocalDate.now())
                .orden(numeroDia)
                .actividades(new ArrayList<>())
                .build();
        itinerario.getDias().add(dia);

        ItinerarioActividad actividad = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre("Actividad Original")
                .categoriaTuristica(categoria)
                .provincia(provincia)
                .puntuacionAmbientalEstimada(ecoScoreOriginal)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(orden)
                .build();
        dia.getActividades().add(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));
        when(actividadRepository.save(any(ItinerarioActividad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(Itinerario.class)))
                .thenReturn(new ItinerarioResponseDTO());

        // Build valid substitution request (same category and province)
        SustitucionRequestDTO request = new SustitucionRequestDTO();
        request.setNombre(nombreAlternativa);
        request.setDescripcion("Descripcion alternativa");
        request.setCostoAproximado(BigDecimal.valueOf(10000));
        request.setMoneda("CRC");
        request.setEstablecimientoRecomendado("Hotel Eco");
        request.setEcoScore(ecoScoreAlternativa);
        request.setCategoriaTuristica(categoria.name());
        request.setProvincia(provincia.name());

        // Act
        service.sustituirActividad(itinerarioId, actividadId, request, usuarioId);

        // Assert — structural position preserved
        assertThat(actividad.getOrden())
                .as("Orden should remain unchanged after substitution")
                .isEqualTo(orden);

        assertThat(actividad.getItinerarioDia())
                .as("ItinerarioDia should remain the same after substitution")
                .isSameAs(dia);

        assertThat(actividad.getItinerarioDia().getId())
                .as("ItinerarioDia ID should remain unchanged")
                .isEqualTo(diaId);

        // Assert — score updated
        assertThat(actividad.getPuntuacionAmbientalEstimada())
                .as("puntuacionAmbientalEstimada should equal the alternative's ecoScore")
                .isEqualTo(ecoScoreAlternativa);
    }

    // ========================================================================
    // Property 6: Substitution rejects non-equivalent alternatives
    // ========================================================================

    /**
     * Validates: Requirements 4.2
     *
     * For any substitution attempt where the alternative's category differs from
     * the original activity's categoriaTuristica, the operation SHALL be rejected
     * and the itinerario SHALL remain unchanged.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 6: Substitution rejects non-equivalent alternatives")
    void sustitucionRechazaCategoriaDiferente(
            @ForAll("categoriaArbitraria") InteresTuristico categoriaOriginal,
            @ForAll("categoriaArbitraria") InteresTuristico categoriaAlternativa,
            @ForAll("provinciaArbitraria") Provincia provincia,
            @ForAll("ecoScoreArbitrario") int ecoScoreOriginal,
            @ForAll("nombreArbitrario") String nombreAlternativa) {

        // Only test when categories are actually different
        Assume.that(!categoriaOriginal.equals(categoriaAlternativa));

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper, mock(jakarta.persistence.EntityManager.class));

        // Build itinerario structure
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

        ItinerarioActividad actividad = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre("Actividad Original")
                .categoriaTuristica(categoriaOriginal)
                .provincia(provincia)
                .puntuacionAmbientalEstimada(ecoScoreOriginal)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(1)
                .build();
        dia.getActividades().add(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));

        // Build request with DIFFERENT category (same province)
        SustitucionRequestDTO request = new SustitucionRequestDTO();
        request.setNombre(nombreAlternativa);
        request.setDescripcion("Descripcion");
        request.setCostoAproximado(BigDecimal.valueOf(5000));
        request.setMoneda("CRC");
        request.setEstablecimientoRecomendado("Hotel X");
        request.setEcoScore(80);
        request.setCategoriaTuristica(categoriaAlternativa.name());
        request.setProvincia(provincia.name());

        // Act & Assert — should be rejected
        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("La alternativa seleccionada no es equivalente a la actividad original.");
                });

        // Assert — activity remains unchanged
        assertThat(actividad.getNombre())
                .as("Activity name should remain unchanged after rejected substitution")
                .isEqualTo("Actividad Original");
        assertThat(actividad.getPuntuacionAmbientalEstimada())
                .as("EcoScore should remain unchanged after rejected substitution")
                .isEqualTo(ecoScoreOriginal);
    }

    /**
     * Validates: Requirements 4.2
     *
     * For any substitution attempt where the alternative's provincia differs from
     * the original activity's provincia, the operation SHALL be rejected
     * and the itinerario SHALL remain unchanged.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 6: Substitution rejects non-equivalent alternatives")
    void sustitucionRechazaProvinciaDiferente(
            @ForAll("categoriaArbitraria") InteresTuristico categoria,
            @ForAll("provinciaArbitraria") Provincia provinciaOriginal,
            @ForAll("provinciaArbitraria") Provincia provinciaAlternativa,
            @ForAll("ecoScoreArbitrario") int ecoScoreOriginal,
            @ForAll("nombreArbitrario") String nombreAlternativa) {

        // Only test when provinces are actually different
        Assume.that(!provinciaOriginal.equals(provinciaAlternativa));

        // Arrange
        UUID itinerarioId = UUID.randomUUID();
        UUID actividadId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper, mock(jakarta.persistence.EntityManager.class));

        // Build itinerario structure
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

        ItinerarioActividad actividad = ItinerarioActividad.builder()
                .id(actividadId)
                .itinerarioDia(dia)
                .nombre("Actividad Original")
                .categoriaTuristica(categoria)
                .provincia(provinciaOriginal)
                .puntuacionAmbientalEstimada(ecoScoreOriginal)
                .horario(LocalTime.of(9, 0))
                .duracionMinutos(60)
                .orden(1)
                .build();
        dia.getActividades().add(actividad);

        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.of(actividad));

        // Build request with same category but DIFFERENT province
        SustitucionRequestDTO request = new SustitucionRequestDTO();
        request.setNombre(nombreAlternativa);
        request.setDescripcion("Descripcion");
        request.setCostoAproximado(BigDecimal.valueOf(5000));
        request.setMoneda("CRC");
        request.setEstablecimientoRecomendado("Hotel Y");
        request.setEcoScore(85);
        request.setCategoriaTuristica(categoria.name());
        request.setProvincia(provinciaAlternativa.name());

        // Act & Assert — should be rejected
        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("La alternativa seleccionada no es equivalente a la actividad original.");
                });

        // Assert — activity remains unchanged
        assertThat(actividad.getNombre())
                .as("Activity name should remain unchanged after rejected substitution")
                .isEqualTo("Actividad Original");
        assertThat(actividad.getPuntuacionAmbientalEstimada())
                .as("EcoScore should remain unchanged after rejected substitution")
                .isEqualTo(ecoScoreOriginal);
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
    Arbitrary<Integer> ordenArbitrario() {
        return Arbitraries.integers().between(1, 20);
    }

    @Provide
    Arbitrary<Integer> numeroDiaArbitrario() {
        return Arbitraries.integers().between(1, 14);
    }

    @Provide
    Arbitrary<Integer> ecoScoreArbitrario() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<String> nombreArbitrario() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withChars(' ')
                .ofMinLength(3)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());
    }
}
