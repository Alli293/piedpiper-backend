package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for CalificacionCreacionService — Property 3: Rechazo por estado de auditoría no terminal.
 *
 * **Validates: Requirements 1.4, 9.2**
 *
 * For any estado de auditoría diferente a CERTIFICACION_EMITIDA (es decir, cualquiera de
 * SOLICITUD_ENVIADA, AUDITOR_ASIGNADO, EN_REVISION, REPORTE_CARGADO, OBSERVACIONES_PENDIENTES),
 * un intento de creación de calificación SHALL ser rechazado con HTTP 422.
 */
class CalificacionRechazoEstadoPropertyTest {

    /**
     * **Validates: Requirements 1.4, 9.2**
     *
     * For any audit state different from CERTIFICACION_EMITIDA, the creation service
     * SHALL throw ApiException with UNPROCESSABLE_ENTITY (422) status.
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 3: Rechazo por estado de auditoría no terminal")
    void estadoNoTerminal_rechazaConHttp422(
            @ForAll("estadoNoTerminal") EstadoSolicitudAuditoria estado,
            @ForAll("uuidArbitrario") UUID auditoriaId,
            @ForAll("uuidArbitrario") UUID usuarioId,
            @ForAll("uuidArbitrario") UUID empresaId,
            @ForAll("calificacionValida") int calificacion) {

        // Arrange: mock all dependencies
        CalificacionRepository calificacionRepository = mock(CalificacionRepository.class);
        SolicitudAuditoriaRepository solicitudAuditoriaRepository = mock(SolicitudAuditoriaRepository.class);
        PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        CalificacionMapper calificacionMapper = mock(CalificacionMapper.class);

        CalificacionCreacionService service = new CalificacionCreacionService(
                calificacionRepository,
                solicitudAuditoriaRepository,
                perfilAuditorRepository,
                usuarioRepository,
                calificacionMapper);

        // Mock authentication
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(usuarioId.toString());

        // Mock usuario with empresa
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(empresaId);

        Usuario usuario = mock(Usuario.class);
        when(usuario.getEmpresa()).thenReturn(empresa);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Mock auditoria with non-terminal state and same empresa (so permission check passes)
        Empresa empresaAuditoria = mock(Empresa.class);
        when(empresaAuditoria.getId()).thenReturn(empresaId);

        SolicitudAuditoria auditoria = mock(SolicitudAuditoria.class);
        when(auditoria.getEstado()).thenReturn(estado);
        when(auditoria.getEmpresa()).thenReturn(empresaAuditoria);

        when(solicitudAuditoriaRepository.findById(auditoriaId)).thenReturn(Optional.of(auditoria));

        // Build request DTO
        CrearCalificacionRequestDTO request = new CrearCalificacionRequestDTO(
                auditoriaId, calificacion, null);

        // Act & Assert: service should reject with 422
        assertThatThrownBy(() -> service.crear(request, authentication))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("Solo se pueden calificar auditorías con certificación emitida.");
                });
    }

    // ========================================================================
    // Arbitraries
    // ========================================================================

    @Provide
    Arbitrary<EstadoSolicitudAuditoria> estadoNoTerminal() {
        return Arbitraries.of(
                EstadoSolicitudAuditoria.SOLICITUD_ENVIADA,
                EstadoSolicitudAuditoria.AUDITOR_ASIGNADO,
                EstadoSolicitudAuditoria.EN_REVISION,
                EstadoSolicitudAuditoria.REPORTE_CARGADO,
                EstadoSolicitudAuditoria.OBSERVACIONES_PENDIENTES
        );
    }

    @Provide
    Arbitrary<UUID> uuidArbitrario() {
        return Arbitraries.longs().tuple2()
                .map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<Integer> calificacionValida() {
        return Arbitraries.integers().between(1, 5);
    }
}
