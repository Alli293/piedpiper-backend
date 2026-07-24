package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.ResultadoPerfil;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.models.enums.Rol;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Feature: PP-54-gestion-especialidades-auditor, Property 5: Specialties catalog membership validation
// Validates: Requirements 3.2
class AuditorPerfilServiceSpecialtiesMembershipPropertyTest {

    private static final Set<String> VALID_ESPECIALIDADES = Arrays.stream(EspecialidadAuditor.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private final UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
    private final PerfilAuditorRepository perfilAuditorRepository = Mockito.mock(PerfilAuditorRepository.class);
    private final PerfilAuditorMapper perfilAuditorMapper = Mockito.mock(PerfilAuditorMapper.class);

    private final AuditorPerfilService service = new AuditorPerfilService(
            perfilAuditorRepository, usuarioRepository, perfilAuditorMapper);

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void invalidSpecialtiesAreRejectedWithBadRequest(
            @ForAll("invalidEspecialidades") List<String> invalidEspecialidades,
            @ForAll("uuids") UUID id) {

        Usuario usuario = Usuario.builder()
                .id(id)
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                invalidEspecialidades,
                List.of(ProvinciaCR.SAN_JOSE.name()),
                true,
                "Descripción válida"
        );

        assertThatThrownBy(() -> service.actualizar(id, id, dto))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage())
                            .contains("Las siguientes especialidades no son válidas:");
                });
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void validSpecialtiesPassMembershipCheck(
            @ForAll("validEspecialidadesList") List<String> validEspecialidades,
            @ForAll("uuids") UUID id) {

        Usuario usuario = Usuario.builder()
                .id(id)
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        when(perfilAuditorRepository.findByAuditorId(id)).thenReturn(Optional.of(
                PerfilAuditor.builder().auditor(usuario).build()));
        when(perfilAuditorRepository.save(any(PerfilAuditor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(perfilAuditorMapper.aResponseDto(any(PerfilAuditor.class)))
                .thenReturn(new PerfilAuditorResponseDTO());

        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                validEspecialidades,
                List.of(ProvinciaCR.SAN_JOSE.name()),
                true,
                "Descripción válida"
        );

        // Should NOT throw any exception related to membership validation
        ResultadoPerfil result = service.actualizar(id, id, dto);
        assertThat(result).isNotNull();
    }

    @Provide
    Arbitrary<List<String>> invalidEspecialidades() {
        // Generate 1-3 strings that are NOT valid enum values
        Arbitrary<String> invalidString = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> !VALID_ESPECIALIDADES.contains(s));

        return invalidString.list().ofMinSize(1).ofMaxSize(3);
    }

    @Provide
    Arbitrary<List<String>> validEspecialidadesList() {
        // Generate lists of 1-8 valid Especialidad enum values
        return Arbitraries.of(EspecialidadAuditor.values())
                .map(Enum::name)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements();
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }
}
