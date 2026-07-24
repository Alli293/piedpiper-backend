package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ZonaCobertura;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.user.models.enums.EstadoUsuario;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Feature: PP-54-gestion-especialidades-auditor, Property 3: Non-active state rejects update
// Validates: Requirements 2.2
class AuditorPerfilServiceNonActiveStatePropertyTest {

    private final UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
    private final PerfilAuditorRepository perfilAuditorRepository = Mockito.mock(PerfilAuditorRepository.class);
    private final PerfilAuditorMapper perfilAuditorMapper = Mockito.mock(PerfilAuditorMapper.class);

    private final AuditorPerfilService service = new AuditorPerfilService(
            perfilAuditorRepository, usuarioRepository, perfilAuditorMapper);

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void nonActiveStateAlwaysRejectsUpdateWith403(
            @ForAll("nonActiveStates") EstadoUsuario estado,
            @ForAll("uuids") UUID id) {

        Usuario usuario = Usuario.builder()
                .id(id)
                .estado(estado)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of(EspecialidadAuditor.ENERGIA_RENOVABLE.name()),
                List.of(ZonaCobertura.SAN_JOSE.name()),
                true,
                "Descripción de prueba"
        );

        assertThatThrownBy(() -> service.actualizar(id, id, dto))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage())
                            .isEqualTo("Tu cuenta debe estar validada para actualizar tu perfil de directorio.");
                });
    }

    @Provide
    Arbitrary<EstadoUsuario> nonActiveStates() {
        return Arbitraries.of(
                EstadoUsuario.PENDIENTE_VALIDACION,
                EstadoUsuario.PENDIENTE_VERIFICACION,
                EstadoUsuario.RECHAZADO,
                EstadoUsuario.DESHABILITADO
        );
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }
}
