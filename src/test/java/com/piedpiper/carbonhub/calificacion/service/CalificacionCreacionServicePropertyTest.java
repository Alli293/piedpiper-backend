package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
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

import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for CalificacionCreacionService.
 *
 * **Validates: Requirements 1.2, 8.1, 8.5**
 */
class CalificacionCreacionServicePropertyTest {

    /**
     * Property 1: Cálculo del promedio como media aritmética
     *
     * For any lista no vacía de calificaciones (valores enteros entre 1 y 5) asociadas a un auditor,
     * la calificacionPromedio almacenada en perfiles_auditor SHALL ser igual a la media aritmética
     * de todos esos valores, redondeada a 1 decimal (half-up).
     *
     * **Validates: Requirements 1.2, 8.1, 8.5**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 1: Cálculo del promedio como media aritmética")
    void promedioEsMediaAritmeticaRedondeadaA1Decimal(
            @ForAll("calificacionesValidas") List<Integer> calificaciones) {

        // Arrange
        UUID auditorId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        UUID auditoriaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        // Calculate expected average: arithmetic mean of all ratings, rounded to 1 decimal HALF_UP
        double sum = calificaciones.stream().mapToInt(Integer::intValue).sum();
        double rawAverage = sum / calificaciones.size();
        BigDecimal expectedPromedio = BigDecimal.valueOf(rawAverage)
                .setScale(1, RoundingMode.HALF_UP);

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

        PerfilAuditor perfilAuditor = PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .build();

        // Mock the repository to return the average that the DB would calculate for this set
        when(calificacionRepository.promedioByAuditorId(auditorId))
                .thenReturn(Optional.of(rawAverage));
        when(calificacionRepository.existsByAuditoriaIdAndEmpresaId(auditoriaId, empresaId))
                .thenReturn(false);
        when(calificacionRepository.save(any(Calificacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(solicitudAuditoriaRepository.findById(auditoriaId))
                .thenReturn(Optional.of(auditoria));
        when(perfilAuditorRepository.findByAuditorId(auditorId))
                .thenReturn(Optional.of(perfilAuditor));
        when(perfilAuditorRepository.save(any(PerfilAuditor.class)))
                .thenAnswer(inv -> inv.getArgument(0));
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

        // Assert: capture the perfil saved and verify the promedio matches expected
        ArgumentCaptor<PerfilAuditor> perfilCaptor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(perfilCaptor.capture());

        BigDecimal actualPromedio = perfilCaptor.getValue().getCalificacionPromedio();
        assertThat(actualPromedio)
                .as("Promedio para calificaciones %s debe ser %s (media aritmética redondeada HALF_UP a 1 decimal)",
                        calificaciones, expectedPromedio)
                .isEqualByComparingTo(expectedPromedio);
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
