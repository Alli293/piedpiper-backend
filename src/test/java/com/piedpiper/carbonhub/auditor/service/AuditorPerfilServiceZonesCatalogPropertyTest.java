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
import net.jqwik.api.Assume;
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
import static org.mockito.Mockito.when;

// Feature: PP-54-gestion-especialidades-auditor, Property 6: Zones catalog membership validation
// Validates: Requirements 3.3, 3.4
class AuditorPerfilServiceZonesCatalogPropertyTest {

    private static final Set<String> ZONAS_VALIDAS = Arrays.stream(ZonaCobertura.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private final UsuarioRepository usuarioRepository = Mockito.mock(UsuarioRepository.class);
    private final PerfilAuditorRepository perfilAuditorRepository = Mockito.mock(PerfilAuditorRepository.class);
    private final PerfilAuditorMapper perfilAuditorMapper = Mockito.mock(PerfilAuditorMapper.class);

    private final AuditorPerfilService service = new AuditorPerfilService(
            perfilAuditorRepository, usuarioRepository, perfilAuditorMapper);

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    void invalidZonasAlwaysRejectedWithBadRequest(@ForAll("invalidZonaStrings") String invalidZona) {

        Assume.that(!ZONAS_VALIDAS.contains(invalidZona));

        UUID id = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(id)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        ActualizarPerfilAuditorRequestDTO dto = new ActualizarPerfilAuditorRequestDTO(
                List.of(EspecialidadAuditor.ENERGIA_RENOVABLE.name()),
                List.of(invalidZona),
                true,
                "Descripción de prueba"
        );

        assertThatThrownBy(() -> service.actualizar(id, id, dto))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getMessage())
                            .contains("Las siguientes zonas de cobertura no son válidas:");
                });
    }

    @Provide
    Arbitrary<String> invalidZonaStrings() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }
}
