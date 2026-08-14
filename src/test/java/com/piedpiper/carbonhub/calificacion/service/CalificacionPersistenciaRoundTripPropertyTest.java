package com.piedpiper.carbonhub.calificacion.service;

import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.EstadoSolicitudAuditoria;
import com.piedpiper.carbonhub.auditoria.repository.SolicitudAuditoriaRepository;
import com.piedpiper.carbonhub.calificacion.mappers.CalificacionMapper;
import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.CrearCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.dtos.EditarCalificacionRequestDTO;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;
import com.piedpiper.carbonhub.calificacion.repository.CalificacionRepository;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for CalificacionCreacionService and CalificacionEdicionService.
 *
 * Property 2: Persistencia round-trip de calificación
 *
 * For any valor de calificación entero en [1, 5] y comentario de 0 a 500 caracteres (o null),
 * si la creación o edición es exitosa, los campos calificacion y comentario recuperados del
 * registro persistido SHALL ser idénticos a los enviados en la petición.
 *
 * Validates: Requirements 1.1, 2.1
 */
class CalificacionPersistenciaRoundTripPropertyTest {

    private CalificacionRepository calificacionRepository;
    private SolicitudAuditoriaRepository solicitudAuditoriaRepository;
    private PerfilAuditorRepository perfilAuditorRepository;
    private UsuarioRepository usuarioRepository;
    private CalificacionMapper calificacionMapper;
    private CalificacionCreacionService creacionService;
    private CalificacionEdicionService edicionService;
    private Authentication authentication;

    // Fixed UUIDs for the test scenario
    private UUID usuarioId;
    private UUID empresaId;
    private UUID auditoriaId;
    private UUID auditorId;

    @BeforeTry
    void setUp() {
        calificacionRepository = mock(CalificacionRepository.class);
        solicitudAuditoriaRepository = mock(SolicitudAuditoriaRepository.class);
        perfilAuditorRepository = mock(PerfilAuditorRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        calificacionMapper = mock(CalificacionMapper.class);
        authentication = mock(Authentication.class);

        usuarioId = UUID.randomUUID();
        empresaId = UUID.randomUUID();
        auditoriaId = UUID.randomUUID();
        auditorId = UUID.randomUUID();

        creacionService = new CalificacionCreacionService(
                calificacionRepository,
                solicitudAuditoriaRepository,
                perfilAuditorRepository,
                usuarioRepository,
                calificacionMapper
        );

        edicionService = new CalificacionEdicionService(
                calificacionRepository,
                perfilAuditorRepository,
                usuarioRepository,
                calificacionMapper
        );

        // Authentication returns the user's UUID
        when(authentication.getName()).thenReturn(usuarioId.toString());

        // Setup empresa and usuario
        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setEmpresa(empresa);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Setup auditoria in correct state with matching empresa
        Usuario auditor = new Usuario();
        auditor.setId(auditorId);

        SolicitudAuditoria auditoria = new SolicitudAuditoria();
        auditoria.setId(auditoriaId);
        auditoria.setEmpresa(empresa);
        auditoria.setEstado(EstadoSolicitudAuditoria.CERTIFICACION_EMITIDA);
        auditoria.setAuditor(auditor);

        when(solicitudAuditoriaRepository.findById(auditoriaId)).thenReturn(Optional.of(auditoria));

        // No duplicate calificacion exists
        when(calificacionRepository.existsByAuditoriaIdAndEmpresaId(auditoriaId, empresaId))
                .thenReturn(false);

        // Repository save returns the entity as-is (simulating persistence)
        when(calificacionRepository.save(any(Calificacion.class)))
                .thenAnswer(invocation -> {
                    Calificacion entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }
                    return entity;
                });

        // El recálculo de métricas del auditor (perfilAuditorRepository.actualizarMetricasCalificacion)
        // es un UPDATE atómico de retorno void; no necesita stub, el mock no-opea por defecto.

        // Mapper delegates to a real-like implementation that preserves calificacion and comentario
        when(calificacionMapper.toDto(any(Calificacion.class))).thenAnswer(invocation -> {
            Calificacion entity = invocation.getArgument(0);
            CalificacionResponseDTO dto = new CalificacionResponseDTO();
            dto.setId(entity.getId());
            dto.setAuditoriaId(entity.getAuditoria() != null ? entity.getAuditoria().getId() : null);
            dto.setAuditorId(entity.getAuditor() != null ? entity.getAuditor().getId() : null);
            dto.setEmpresaId(entity.getEmpresa() != null ? entity.getEmpresa().getId() : null);
            dto.setCalificacion(entity.getCalificacion());
            dto.setComentario(entity.getComentario());
            dto.setCreadoEn(entity.getCreadoEn());
            dto.setActualizadoEn(entity.getActualizadoEn());
            return dto;
        });
    }

    // =========================================================================
    // Property 2a: Round-trip de creación — calificacion y comentario se preservan
    // =========================================================================

    /**
     * Property 2: Persistencia round-trip de calificación (Creación)
     *
     * For any calificación value in [1,5] and any comentario of 0-500 chars,
     * the CalificacionResponseDTO returned by crear() SHALL have identical
     * calificacion and comentario values to those sent in the request.
     *
     * **Validates: Requirements 1.1**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 2: Persistencia round-trip de calificación")
    void creacion_roundTrip_calificacionYComentarioSePreservan(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValor,
            @ForAll("comentariosValidos") String comentario
    ) {
        CrearCalificacionRequestDTO request = new CrearCalificacionRequestDTO();
        request.setAuditoriaId(auditoriaId);
        request.setCalificacion(calificacionValor);
        request.setComentario(comentario);

        CalificacionResponseDTO response = creacionService.crear(request, authentication);

        assertThat(response.getCalificacion())
                .as("La calificación en la respuesta debe ser idéntica a la enviada")
                .isEqualTo(calificacionValor);

        assertThat(response.getComentario())
                .as("El comentario en la respuesta debe ser idéntico al enviado")
                .isEqualTo(comentario);
    }

    /**
     * Property 2: Persistencia round-trip de calificación (Creación con comentario null)
     *
     * For any calificación value in [1,5] with a null comentario,
     * the response SHALL have calificacion identical and comentario null.
     *
     * **Validates: Requirements 1.1**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 2: Persistencia round-trip de calificación")
    void creacion_roundTrip_comentarioNullSePreserva(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValor
    ) {
        CrearCalificacionRequestDTO request = new CrearCalificacionRequestDTO();
        request.setAuditoriaId(auditoriaId);
        request.setCalificacion(calificacionValor);
        request.setComentario(null);

        CalificacionResponseDTO response = creacionService.crear(request, authentication);

        assertThat(response.getCalificacion())
                .as("La calificación en la respuesta debe ser idéntica a la enviada")
                .isEqualTo(calificacionValor);

        assertThat(response.getComentario())
                .as("El comentario null debe preservarse como null")
                .isNull();
    }

    // =========================================================================
    // Property 2b: Round-trip de edición — calificacion y comentario se preservan
    // =========================================================================

    /**
     * Property 2: Persistencia round-trip de calificación (Edición)
     *
     * For any calificación value in [1,5] and any comentario of 0-500 chars,
     * the CalificacionResponseDTO returned by editar() SHALL have identical
     * calificacion and comentario values to those sent in the request.
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 2: Persistencia round-trip de calificación")
    void edicion_roundTrip_calificacionYComentarioSePreservan(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValor,
            @ForAll("comentariosValidos") String comentario
    ) {
        UUID calificacionId = UUID.randomUUID();

        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        Usuario auditor = new Usuario();
        auditor.setId(auditorId);

        SolicitudAuditoria auditoria = new SolicitudAuditoria();
        auditoria.setId(auditoriaId);

        Calificacion existente = Calificacion.builder()
                .id(calificacionId)
                .auditoria(auditoria)
                .auditor(auditor)
                .empresa(empresa)
                .calificacion(3)
                .comentario("comentario anterior")
                .creadoEn(Instant.now().minusSeconds(3600))
                .actualizadoEn(Instant.now().minusSeconds(3600))
                .build();

        when(calificacionRepository.findById(calificacionId)).thenReturn(Optional.of(existente));

        EditarCalificacionRequestDTO request = new EditarCalificacionRequestDTO();
        request.setCalificacion(calificacionValor);
        request.setComentario(comentario);

        CalificacionResponseDTO response = edicionService.editar(calificacionId, request, authentication);

        assertThat(response.getCalificacion())
                .as("La calificación editada en la respuesta debe ser idéntica a la enviada")
                .isEqualTo(calificacionValor);

        assertThat(response.getComentario())
                .as("El comentario editado en la respuesta debe ser idéntico al enviado")
                .isEqualTo(comentario);
    }

    /**
     * Property 2: Persistencia round-trip de calificación (Edición con comentario null)
     *
     * For any calificación value in [1,5] with a null comentario,
     * the response SHALL have calificacion identical and comentario null.
     *
     * **Validates: Requirements 2.1**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 2: Persistencia round-trip de calificación")
    void edicion_roundTrip_comentarioNullSePreserva(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValor
    ) {
        UUID calificacionId = UUID.randomUUID();

        Empresa empresa = new Empresa();
        empresa.setId(empresaId);

        Usuario auditor = new Usuario();
        auditor.setId(auditorId);

        SolicitudAuditoria auditoria = new SolicitudAuditoria();
        auditoria.setId(auditoriaId);

        Calificacion existente = Calificacion.builder()
                .id(calificacionId)
                .auditoria(auditoria)
                .auditor(auditor)
                .empresa(empresa)
                .calificacion(2)
                .comentario("viejo")
                .creadoEn(Instant.now().minusSeconds(3600))
                .actualizadoEn(Instant.now().minusSeconds(3600))
                .build();

        when(calificacionRepository.findById(calificacionId)).thenReturn(Optional.of(existente));

        EditarCalificacionRequestDTO request = new EditarCalificacionRequestDTO();
        request.setCalificacion(calificacionValor);
        request.setComentario(null);

        CalificacionResponseDTO response = edicionService.editar(calificacionId, request, authentication);

        assertThat(response.getCalificacion())
                .as("La calificación editada en la respuesta debe ser idéntica a la enviada")
                .isEqualTo(calificacionValor);

        assertThat(response.getComentario())
                .as("El comentario null debe preservarse como null en edición")
                .isNull();
    }

    // =========================================================================
    // Property 2c: El entity persistido contiene exactamente los valores enviados
    // =========================================================================

    /**
     * Property 2: Persistencia round-trip — verificación a nivel de entidad persistida (Creación)
     *
     * For any calificación value in [1,5] and any comentario (0-500 chars or null),
     * the Calificacion entity passed to repository.save() SHALL contain
     * calificacion and comentario fields identical to the request values.
     *
     * **Validates: Requirements 1.1**
     */
    @Property(tries = 100)
    @Tag("Feature: PP-56-calificacion-verificada-auditores, Property 2: Persistencia round-trip de calificación")
    void creacion_entityPersistida_contieneValoresDelRequest(
            @ForAll @IntRange(min = 1, max = 5) int calificacionValor,
            @ForAll("comentariosONull") String comentario
    ) {
        CrearCalificacionRequestDTO request = new CrearCalificacionRequestDTO();
        request.setAuditoriaId(auditoriaId);
        request.setCalificacion(calificacionValor);
        request.setComentario(comentario);

        creacionService.crear(request, authentication);

        ArgumentCaptor<Calificacion> captor = ArgumentCaptor.forClass(Calificacion.class);
        verify(calificacionRepository).save(captor.capture());
        Calificacion saved = captor.getValue();

        assertThat(saved.getCalificacion())
                .as("El valor calificacion en la entidad persistida debe ser idéntico al del request")
                .isEqualTo(calificacionValor);

        assertThat(saved.getComentario())
                .as("El comentario en la entidad persistida debe ser idéntico al del request")
                .isEqualTo(comentario);
    }

    // =========================================================================
    // Providers
    // =========================================================================

    /**
     * Genera comentarios válidos de 0 a 500 caracteres (cadenas no-null).
     */
    @Provide
    Arbitrary<String> comentariosValidos() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', 'á', 'é', 'í', 'ó', 'ú', 'ñ')
                .ofMinLength(0)
                .ofMaxLength(500);
    }

    /**
     * Genera comentarios válidos (0-500 chars) o null.
     */
    @Provide
    Arbitrary<String> comentariosONull() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                comentariosValidos()
        );
    }
}
