package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapperImpl;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Feature: PP-54-gestion-especialidades-auditor, Property 1: Profile update round trip
// Validates: Requirements 1.1
class AuditorPerfilServiceRoundTripPropertyTest {

    private final PerfilAuditorRepository perfilAuditorRepository = Mockito.mock(PerfilAuditorRepository.class);
    private final UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
    private final PerfilAuditorMapper perfilAuditorMapper = new PerfilAuditorMapperImpl();

    private final AuditorPerfilService service = new AuditorPerfilService(
            perfilAuditorRepository, usuarioRepository, perfilAuditorMapper);

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void profileUpdateRoundTrip_responseContainsExactSameValues(
            @ForAll("validDtos") ActualizarPerfilAuditorRequestDTO dto) {

        UUID id = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(id)
                .estado(EstadoUsuario.ACTIVO)
                .rol(Rol.AUDITOR_CERTIFICADO)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        PerfilAuditor existingPerfil = PerfilAuditor.builder()
                .auditor(usuario)
                .build();

        when(perfilAuditorRepository.findByAuditorId(id)).thenReturn(Optional.of(existingPerfil));
        when(perfilAuditorRepository.save(any(PerfilAuditor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerfilAuditorResponseDTO response = service.actualizar(id, id, dto).dto();

        assertThat(response.getEspecialidades())
                .containsExactlyInAnyOrderElementsOf(dto.getEspecialidades());
        assertThat(response.getZonasCobertura())
                .containsExactlyInAnyOrderElementsOf(dto.getZonasCobertura());
        assertThat(response.isDisponible()).isEqualTo(dto.getDisponible());
        assertThat(response.getDescripcionProfesional()).isEqualTo(dto.getDescripcionProfesional());
    }

    @Provide
    Arbitrary<ActualizarPerfilAuditorRequestDTO> validDtos() {
        Arbitrary<List<String>> especialidades = Arbitraries.of(
                Arrays.stream(EspecialidadAuditor.values()).map(Enum::name).toList()
        ).list().ofMinSize(1).ofMaxSize(5).uniqueElements();

        Arbitrary<List<String>> zonas = Arbitraries.of(
                Arrays.stream(ProvinciaCR.values()).map(Enum::name).toList()
        ).list().ofMinSize(1).ofMaxSize(7).uniqueElements();

        Arbitrary<Boolean> disponible = Arbitraries.of(true, false);

        Arbitrary<String> descripcion = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(500)
        );

        return Combinators.combine(especialidades, zonas, disponible, descripcion)
                .as(ActualizarPerfilAuditorRequestDTO::new);
    }
}
