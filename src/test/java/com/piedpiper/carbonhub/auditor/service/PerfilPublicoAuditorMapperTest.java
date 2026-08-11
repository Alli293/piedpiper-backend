package com.piedpiper.carbonhub.auditor.service;

import com.piedpiper.carbonhub.auditor.mappers.PerfilPublicoAuditorMapper;
import com.piedpiper.carbonhub.auditor.models.dtos.PerfilPublicoAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.ProvinciaCR;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilPublicoAuditorMapperTest {

    private final PerfilPublicoAuditorMapper mapper = new PerfilPublicoAuditorMapper() {
    };

    @Test
    void mapeaProvinciaDelPerfilPublico() {
        PerfilAuditor perfil = PerfilAuditor.builder()
                .auditor(Usuario.builder()
                        .id(UUID.randomUUID())
                        .nombre("Juana")
                        .apellidos("Rojas")
                        .build())
                .provincia(ProvinciaCR.HEREDIA)
                .build();

        PerfilPublicoAuditorResponseDTO dto = mapper.aPerfilPublicoDto(
                perfil,
                null,
                List.of(),
                List.of(),
                List.of());

        assertThat(dto.getProvincia()).isEqualTo("HEREDIA");
    }
}
