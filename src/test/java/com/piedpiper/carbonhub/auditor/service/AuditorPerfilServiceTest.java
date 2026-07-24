package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditorPerfilServiceTest {

    @Mock
    private PerfilAuditorRepository perfilAuditorRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilAuditorMapper perfilAuditorMapper;

    @InjectMocks
    private AuditorPerfilService service;

    private static final UUID AUDITOR_ID = UUID.randomUUID();

    // --- Helpers ---

    private ActualizarPerfilAuditorRequestDTO requestValido() {
        return new ActualizarPerfilAuditorRequestDTO(
                List.of("ENERGIA_RENOVABLE", "AGROINDUSTRIA"),
                List.of("SAN_JOSE", "HEREDIA"),
                true,
                "Auditor con experiencia en energía renovable."
        );
    }

    private Usuario auditorActivo() {
        return Usuario.builder()
                .id(AUDITOR_ID)
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();
    }

    private Usuario auditorConEstado(EstadoUsuario estado) {
        return Usuario.builder()
                .id(AUDITOR_ID)
                .estado(estado)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();
    }

    private PerfilAuditorResponseDTO responseEsperado() {
        return new PerfilAuditorResponseDTO(
                AUDITOR_ID,
                List.of("AGROINDUSTRIA", "ENERGIA_RENOVABLE"),
                List.of("HEREDIA", "SAN_JOSE"),
                true,
                "Auditor con experiencia en energía renovable.",
                Instant.now()
        );
    }

    // --- 1. Ownership check: mismatched IDs → ApiException FORBIDDEN ---

    @Test
    void ownershipCheck_idsMismatch_lanzaForbidden() {
        UUID otroUsuarioId = UUID.randomUUID();

        assertThatThrownBy(() -> service.actualizar(otroUsuarioId, AUDITOR_ID, requestValido()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage()).isEqualTo("No tiene permiso para editar este perfil.");
                });

        verify(usuarioRepository, never()).findById(any());
        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 2. State check: PENDIENTE_VALIDACION → ApiException FORBIDDEN ---

    @Test
    void stateCheck_pendienteValidacion_lanzaForbidden() {
        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditorConEstado(EstadoUsuario.PENDIENTE_VALIDACION)));

        assertThatThrownBy(() -> service.actualizar(AUDITOR_ID, AUDITOR_ID, requestValido()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage()).isEqualTo(
                            "Tu cuenta debe estar validada para actualizar tu perfil de directorio.");
                });

        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 3. State check: RECHAZADO → ApiException FORBIDDEN ---

    @Test
    void stateCheck_rechazado_lanzaForbidden() {
        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditorConEstado(EstadoUsuario.RECHAZADO)));

        assertThatThrownBy(() -> service.actualizar(AUDITOR_ID, AUDITOR_ID, requestValido()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage()).isEqualTo(
                            "Tu cuenta debe estar validada para actualizar tu perfil de directorio.");
                });

        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 4. Role check: ACTIVO but not AUDITOR_CERTIFICADO → ApiException FORBIDDEN ---

    @Test
    void roleCheck_activoNoAuditor_lanzaForbidden() {
        Usuario usuarioGeneral = Usuario.builder()
                .id(AUDITOR_ID)
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.USUARIO_GENERAL)
                .build();

        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(usuarioGeneral));

        assertThatThrownBy(() -> service.actualizar(AUDITOR_ID, AUDITOR_ID, requestValido()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage()).isEqualTo(
                            "Solo usuarios con rol AUDITOR_CERTIFICADO pueden gestionar su perfil.");
                });

        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 5. Catalog membership: invalid especialidad → ApiException BAD_REQUEST ---

    @Test
    void catalogCheck_especialidadInvalida_lanzaBadRequest() {
        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditorActivo()));

        ActualizarPerfilAuditorRequestDTO request = new ActualizarPerfilAuditorRequestDTO(
                List.of("ENERGIA_RENOVABLE", "VALOR_INVENTADO"),
                List.of("SAN_JOSE"),
                true,
                null
        );

        assertThatThrownBy(() -> service.actualizar(AUDITOR_ID, AUDITOR_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).contains("VALOR_INVENTADO");
                });

        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 6. Catalog membership: invalid zona → ApiException BAD_REQUEST ---

    @Test
    void catalogCheck_zonaInvalida_lanzaBadRequest() {
        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditorActivo()));

        ActualizarPerfilAuditorRequestDTO request = new ActualizarPerfilAuditorRequestDTO(
                List.of("ENERGIA_RENOVABLE"),
                List.of("SAN_JOSE", "ZONA_FANTASMA"),
                true,
                null
        );

        assertThatThrownBy(() -> service.actualizar(AUDITOR_ID, AUDITOR_ID, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage()).contains("ZONA_FANTASMA");
                });

        verify(perfilAuditorRepository, never()).save(any());
    }

    // --- 7. Happy path (new profile): creates new, saves, returns mapped DTO ---

    @Test
    void happyPath_perfilNuevo_creaYRetornaDto() {
        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditorActivo()));
        when(perfilAuditorRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Optional.empty());
        when(perfilAuditorRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerfilAuditorResponseDTO expected = responseEsperado();
        when(perfilAuditorMapper.aResponseDto(any())).thenReturn(expected);

        PerfilAuditorResponseDTO result = service.actualizar(AUDITOR_ID, AUDITOR_ID, requestValido());

        ArgumentCaptor<PerfilAuditor> captor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(captor.capture());
        PerfilAuditor saved = captor.getValue();

        assertThat(saved.getEspecialidades()).containsExactlyInAnyOrder(
                EspecialidadAuditor.ENERGIA_RENOVABLE, EspecialidadAuditor.AGROINDUSTRIA);
        assertThat(saved.getZonasCobertura()).containsExactlyInAnyOrder(
                ProvinciaCR.SAN_JOSE, ProvinciaCR.HEREDIA);
        assertThat(saved.isDisponible()).isTrue();
        assertThat(saved.getDescripcionProfesional()).isEqualTo("Auditor con experiencia en energía renovable.");
        assertThat(saved.getActualizadoEn()).isNotNull();
        assertThat(saved.getAuditor().getId()).isEqualTo(AUDITOR_ID);

        assertThat(result).isEqualTo(expected);
    }

    // --- 8. Happy path (existing profile): updates existing, saves, returns mapped DTO ---

    @Test
    void happyPath_perfilExistente_actualizaYRetornaDto() {
        Usuario auditor = auditorActivo();
        PerfilAuditor perfilExistente = PerfilAuditor.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .especialidades(new HashSet<>(Set.of(EspecialidadAuditor.MANUFACTURA)))
                .zonasCobertura(new HashSet<>(Set.of(ProvinciaCR.CARTAGO)))
                .disponible(false)
                .descripcionProfesional("Descripción anterior")
                .actualizadoEn(Instant.now().minusSeconds(3600))
                .build();

        when(usuarioRepository.findById(AUDITOR_ID))
                .thenReturn(Optional.of(auditor));
        when(perfilAuditorRepository.findByAuditorId(AUDITOR_ID))
                .thenReturn(Optional.of(perfilExistente));
        when(perfilAuditorRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerfilAuditorResponseDTO expected = responseEsperado();
        when(perfilAuditorMapper.aResponseDto(any())).thenReturn(expected);

        PerfilAuditorResponseDTO result = service.actualizar(AUDITOR_ID, AUDITOR_ID, requestValido());

        ArgumentCaptor<PerfilAuditor> captor = ArgumentCaptor.forClass(PerfilAuditor.class);
        verify(perfilAuditorRepository).save(captor.capture());
        PerfilAuditor saved = captor.getValue();

        // Verifica que se actualizó el perfil existente (mismo id)
        assertThat(saved.getId()).isEqualTo(perfilExistente.getId());
        assertThat(saved.getEspecialidades()).containsExactlyInAnyOrder(
                EspecialidadAuditor.ENERGIA_RENOVABLE, EspecialidadAuditor.AGROINDUSTRIA);
        assertThat(saved.getZonasCobertura()).containsExactlyInAnyOrder(
                ProvinciaCR.SAN_JOSE, ProvinciaCR.HEREDIA);
        assertThat(saved.isDisponible()).isTrue();
        assertThat(saved.getDescripcionProfesional()).isEqualTo("Auditor con experiencia en energía renovable.");
        assertThat(saved.getActualizadoEn()).isNotNull();

        assertThat(result).isEqualTo(expected);
    }
}
