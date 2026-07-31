package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.ActualizarPerfilAuditorRequestDTO;
import com.piedpiper.carbonhub.auditor.repository.PerfilAuditorRepository;
import com.piedpiper.carbonhub.exceptions.ApiException;
import com.piedpiper.carbonhub.user.repository.UsuarioRepository;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Combinators;
import net.jqwik.api.Arbitraries;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

// Feature: PP-54-gestion-especialidades-auditor, Property 2: Ownership enforcement rejects mismatched IDs
// Validates: Requirements 2.1
class AuditorPerfilServiceOwnershipPropertyTest {

    private final PerfilAuditorRepository perfilAuditorRepository = mock(PerfilAuditorRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PerfilAuditorMapper perfilAuditorMapper = mock(PerfilAuditorMapper.class);

    private final AuditorPerfilService service = new AuditorPerfilService(
            perfilAuditorRepository, usuarioRepository, perfilAuditorMapper);

    @Provide
    Arbitrary<UUID> uuids() {
        return Combinators.combine(
                Arbitraries.longs(),
                Arbitraries.longs()
        ).as(UUID::new);
    }

    @Property
    void ownershipEnforcement_rejectsMismatchedIds(
            @ForAll("uuids") UUID usuarioId,
            @ForAll("uuids") UUID auditorId) {

        Assume.that(!usuarioId.equals(auditorId));

        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of("AGROINDUSTRIA"),
                List.of("SAN_JOSE"),
                true,
                "Descripción de prueba"
        );

        assertThatThrownBy(() -> service.actualizar(usuarioId, auditorId, dto))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getMessage()).isEqualTo("No tiene permiso para editar este perfil.");
                });
    }
}
