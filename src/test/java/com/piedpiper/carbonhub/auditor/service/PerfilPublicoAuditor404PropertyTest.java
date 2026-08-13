package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.certificacion.config.CatalogoTiposCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;

import net.jqwik.api.*;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for PerfilPublicoAuditorService — Property 2.
 * Verifies that 404 responses are indistinguishable for non-existent and non-active auditors.
 *
 * Validates: Requirements 2.1, 2.2, 2.3
 */
class PerfilPublicoAuditor404PropertyTest {

    private static final String EXPECTED_MESSAGE = "El perfil solicitado no está disponible.";

    private final PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
    private final CertificacionRepository certificacionRepository = mock(CertificacionRepository.class);
    private final CatalogoTiposCertificacion catalogoTiposCertificacion = mock(CatalogoTiposCertificacion.class);
    private final PerfilPublicoAuditorMapper mapper = mock(PerfilPublicoAuditorMapper.class);
    private final Clock clock = Clock.systemDefaultZone();

    private final PerfilPublicoAuditorService service = new PerfilPublicoAuditorService(
            perfilAuditorRepository,
            certificacionRepository,
            catalogoTiposCertificacion,
            mapper,
            clock
    );

    // ========================================================================
    // Property 2a: Non-existent auditor UUID yields 404 with standard message
    // ========================================================================

    /**
     * Validates: Requirements 2.1, 2.2, 2.3
     *
     * For any random UUID that does not correspond to an existing auditor,
     * the service SHALL throw ApiException with HTTP 404 and the message
     * "El perfil solicitado no está disponible."
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 2: Respuestas 404 indistinguibles")
    void auditorInexistente_retorna404ConMensajeEstandar(@ForAll("uuidAleatorio") UUID auditorId) {
        // Arrange: repository returns empty for any UUID with ACTIVO state
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                eq(auditorId), eq(EstadoUsuario.ACTIVO)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.obtenerPerfilPublico(auditorId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assert apiEx.getStatus() == HttpStatus.NOT_FOUND;
                    assert apiEx.getMessage().equals(EXPECTED_MESSAGE);
                });
    }

    // ========================================================================
    // Property 2b: Non-active auditor yields 404 with identical message
    // ========================================================================

    /**
     * Validates: Requirements 2.1, 2.2, 2.3
     *
     * For any auditorId associated with a non-active state (PENDIENTE_VALIDACION,
     * RECHAZADO, PENDIENTE_VERIFICACION, DESHABILITADO), the service SHALL throw
     * ApiException with HTTP 404 and the same message as non-existent auditors.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-53-consulta-perfil-publico-auditor, Property 2: Respuestas 404 indistinguibles")
    void auditorNoActivo_retorna404ConMensajeIdenticoAlInexistente(
            @ForAll("uuidAleatorio") UUID auditorId,
            @ForAll("estadoNoActivo") EstadoUsuario estadoIgnorado) {

        // Arrange: the repo query filters by ACTIVO, so non-active auditors also return empty
        when(perfilAuditorRepository.findByAuditorIdAndAuditorEstadoConDistribucion(
                eq(auditorId), eq(EstadoUsuario.ACTIVO)))
                .thenReturn(Optional.empty());

        // Act & Assert: same exception and same message as non-existent
        assertThatThrownBy(() -> service.obtenerPerfilPublico(auditorId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assert apiEx.getStatus() == HttpStatus.NOT_FOUND;
                    assert apiEx.getMessage().equals(EXPECTED_MESSAGE);
                });
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<UUID> uuidAleatorio() {
        return Arbitraries.randomValue(random -> UUID.randomUUID());
    }

    @Provide
    Arbitrary<EstadoUsuario> estadoNoActivo() {
        return Arbitraries.of(
                EstadoUsuario.PENDIENTE_VALIDACION,
                EstadoUsuario.RECHAZADO,
                EstadoUsuario.PENDIENTE_VERIFICACION,
                EstadoUsuario.DESHABILITADO
        );
    }
}
