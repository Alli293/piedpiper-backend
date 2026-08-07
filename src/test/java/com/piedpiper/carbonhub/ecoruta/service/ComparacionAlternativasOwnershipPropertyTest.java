package com.piedpiper.carbonhub.ecoruta.service;

import com.piedpiper.carbonhub.ecoruta.models.dtos.SustitucionRequestDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioActividadRepository;
import com.piedpiper.carbonhub.ecoruta.repository.ItinerarioRepository;
import com.piedpiper.carbonhub.ecoruta.mappers.ItinerarioMapper;
import com.piedpiper.carbonhub.exceptions.ApiException;

import net.jqwik.api.*;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for ComparacionAlternativasService ownership validation using jqwik.
 *
 * **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 4.5**
 *
 * Property 1: Ownership validation denies unauthorized access
 * For any user and any itinerario/activity combination where the itinerario does not belong
 * to the user OR the activity does not belong to the itinerario, the service SHALL deny
 * access with a 403 response.
 */
class ComparacionAlternativasOwnershipPropertyTest {

    // ========================================================================
    // Property 1: Ownership validation denies unauthorized access
    // ========================================================================

    /**
     * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 4.5
     *
     * When the itinerario does not belong to the user (itinerarioRepository returns empty),
     * obtenerAlternativas SHALL throw ApiException with FORBIDDEN status.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 1: Ownership validation denies unauthorized access")
    void obtenerAlternativas_itinerarioNoPertenece_lanzaAccesoDenegado(
            @ForAll("uuidArbitrario") UUID itinerarioId,
            @ForAll("uuidArbitrario") UUID actividadId,
            @ForAll("uuidArbitrario") UUID usuarioId) {

        // Arrange
        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Itinerario does not belong to this user
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerAlternativas(itinerarioId, actividadId, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("No tienes permiso para acceder a este itinerario.");
                });
    }

    /**
     * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 4.5
     *
     * When the itinerario belongs to the user but the activity does not belong to the itinerario
     * (actividadRepository returns empty), obtenerAlternativas SHALL throw ApiException with
     * FORBIDDEN status.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 1: Ownership validation denies unauthorized access")
    void obtenerAlternativas_actividadNoPertenece_lanzaAccesoDenegado(
            @ForAll("uuidArbitrario") UUID itinerarioId,
            @ForAll("uuidArbitrario") UUID actividadId,
            @ForAll("uuidArbitrario") UUID usuarioId) {

        // Arrange
        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Itinerario DOES belong to user (returns a non-empty Itinerario)
        com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario itinerario =
                com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario.builder()
                        .id(itinerarioId)
                        .dias(java.util.List.of())
                        .build();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        // Activity does NOT belong to the itinerario
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerAlternativas(itinerarioId, actividadId, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("No tienes permiso para acceder a este itinerario.");
                });
    }

    /**
     * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 4.5
     *
     * When the itinerario does not belong to the user (itinerarioRepository returns empty),
     * sustituirActividad SHALL throw ApiException with FORBIDDEN status.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 1: Ownership validation denies unauthorized access")
    void sustituirActividad_itinerarioNoPertenece_lanzaAccesoDenegado(
            @ForAll("uuidArbitrario") UUID itinerarioId,
            @ForAll("uuidArbitrario") UUID actividadId,
            @ForAll("uuidArbitrario") UUID usuarioId,
            @ForAll("sustitucionRequest") SustitucionRequestDTO request) {

        // Arrange
        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Itinerario does not belong to this user
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("No tienes permiso para acceder a este itinerario.");
                });
    }

    /**
     * Validates: Requirements 1.2, 1.3, 1.4, 1.5, 4.5
     *
     * When the itinerario belongs to the user but the activity does not belong to the itinerario,
     * sustituirActividad SHALL throw ApiException with FORBIDDEN status.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-92-comparacion-alternativas-ambientales, Property 1: Ownership validation denies unauthorized access")
    void sustituirActividad_actividadNoPertenece_lanzaAccesoDenegado(
            @ForAll("uuidArbitrario") UUID itinerarioId,
            @ForAll("uuidArbitrario") UUID actividadId,
            @ForAll("uuidArbitrario") UUID usuarioId,
            @ForAll("sustitucionRequest") SustitucionRequestDTO request) {

        // Arrange
        ItinerarioRepository itinerarioRepository = mock(ItinerarioRepository.class);
        ItinerarioActividadRepository actividadRepository = mock(ItinerarioActividadRepository.class);
        AlternativasIaClienteService iaService = mock(AlternativasIaClienteService.class);
        ItinerarioMapper mapper = mock(ItinerarioMapper.class);

        ComparacionAlternativasService service = new ComparacionAlternativasService(
                itinerarioRepository, actividadRepository, iaService, mapper);

        // Itinerario DOES belong to user
        com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario itinerario =
                com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario.builder()
                        .id(itinerarioId)
                        .dias(java.util.List.of())
                        .build();
        when(itinerarioRepository.findByIdAndUsuario_Id(itinerarioId, usuarioId))
                .thenReturn(Optional.of(itinerario));

        // Activity does NOT belong to the itinerario
        when(actividadRepository.findByIdAndItinerarioDia_Itinerario_Id(actividadId, itinerarioId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.sustituirActividad(itinerarioId, actividadId, request, usuarioId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("No tienes permiso para acceder a este itinerario.");
                });
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<UUID> uuidArbitrario() {
        return Arbitraries.longs().tuple2()
                .map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<SustitucionRequestDTO> sustitucionRequest() {
        Arbitrary<String> nombres = Arbitraries.strings()
                .withCharRange('A', 'Z')
                .withCharRange('a', 'z')
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());

        Arbitrary<String> descripciones = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withChars(' ', '.', ',')
                .ofMinLength(0)
                .ofMaxLength(200);

        Arbitrary<BigDecimal> costos = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("500000"));

        Arbitrary<String> monedas = Arbitraries.of("CRC", "USD");

        Arbitrary<Integer> ecoScores = Arbitraries.integers().between(0, 100);

        return Combinators.combine(nombres, descripciones, costos, monedas, ecoScores)
                .as((nombre, descripcion, costo, moneda, ecoScore) -> {
                    SustitucionRequestDTO dto = new SustitucionRequestDTO();
                    dto.setNombre(nombre);
                    dto.setDescripcion(descripcion);
                    dto.setCostoAproximado(costo);
                    dto.setMoneda(moneda);
                    dto.setEcoScore(ecoScore);
                    return dto;
                });
    }
}
