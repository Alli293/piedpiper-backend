package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for CalificacionCreacionService.
 *
 * **Validates: Requirements 1.2, 8.1, 8.5**
 */
class CalificacionCreacionServicePropertyTest {

    /**
     * Property 1: Recalculo de metricas del auditor tras cada calificacion creada
     *
     * El promedio y el conteo de reseñas ya no se calculan ni se redondean en memoria — la media
     * aritmética (HALF_UP a 1 decimal) queda a cargo de la columna NUMERIC(2,1) en Postgres, vía el
     * UPDATE atómico de {@code PerfilAuditorRepository.actualizarMetricasCalificacion}. Lo que esta
     * property verifica en el nivel de servicio es que, para cualquier lista no vacía de
     * calificaciones válidas, crear() siempre dispara ese recálculo exactamente una vez, con el id
     * del auditor correcto — la matemática real vive en la DB y se cubre por separado en un test de
     * integración.
     *
     * **Validates: Requirements 1.2, 8.1, 8.5**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 1: Recálculo de métricas del auditor")
    void crearSiempreDisparaRecalculoAtomicoDeMetricasDelAuditor(
            @ForAll("calificacionesValidas") List<Integer> calificaciones) {

        // Arrange
        UUID auditorId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID auditoriaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        // Mock repositories
        CalificacionRepository calificacionRepository = mock(CalificacionRepository.class);
        SolicitudAuditoriaRepository solicitudAuditoriaRepository = mock(SolicitudAuditoriaRepository.class);
        PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        CalificacionMapper calificacionMapper = mock(CalificacionMapper.class);

        // Build test entities
        Empresa empresa = Empresa.builder().id(empresaId).build();
        Usuario auditor = Usuario.builder().id(auditorId).build();
        Usuario usuario = Usuario.builder().id(usuarioId).empresa(empresa).build();

        SolicitudAuditoria auditoria = SolicitudAuditoria.builder()
                .id(auditoriaId)
                .auditor(auditor)
                .empresa(empresa)
                .estado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA)
                .build();

        when(calificacionRepository.existsByAuditoriaIdAndEmpresaId(auditoriaId, empresaId))
                .thenReturn(false);
        when(calificacionRepository.save(any(Calificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(solicitudAuditoriaRepository.findById(auditoriaId))
                .thenReturn(Optional.of(auditoria));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuario));
        when(calificacionMapper.toDto(any(Calificacion.class)))
                .thenReturn(new CalificacionResponseDTO());

        // Build service
        CalificacionCreacionService service = new CalificacionCreacionService(
                calificacionRepository,
                solicitudAuditoriaRepository,
                perfilAuditorRepository,
                usuarioRepository,
                calificacionMapper);

        // Build request (use the last rating in the list as the one being created)
        CrearCalificacionRequestDTO request = new CrearCalificacionRequestDTO();
        request.setAuditoriaId(auditoriaId);
        request.setCalificacion(calificaciones.get(calificaciones.size() - 1));
        request.setComentario("Test comment");

        // Authentication: Autenticaciones.usuarioId calls authentication.getName() and parses UUID
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                usuarioId.toString(), null);

        // Act
        service.crear(request, authentication);

        // Assert: el recálculo atómico se dispara exactamente una vez, para el auditor correcto
        verify(perfilAuditorRepository, times(1)).actualizarMetricasCalificacion(auditorId);
    }

    /**
     * Generates non-empty lists of integers in [1, 5] representing calificaciones.
     */
    @Provide
    Arbitrary<List<Integer>> calificacionesValidas() {
        return Arbitraries.integers().between(1, 5)
                .list()
                .ofMinSize(1)
                .ofMaxSize(50);
    }
}
