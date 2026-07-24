package com.piedpiper.carbonhub.auditor.mappers;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface PerfilAuditorMapper {

    int MAX_ESPECIALIDADES_PRINCIPALES = 3;

    @Mapping(target = "auditorId", source = "auditor.id")
    @Mapping(target = "nombre", source = "auditor", qualifiedByName = "nombreCompleto")
    @Mapping(target = "especialidadesPrincipales", source = "especialidades", qualifiedByName = "principales")
    AuditorResumenResponseDTO aResumen(PerfilAuditor perfil);

    @Named("nombreCompleto")
    default String nombreCompleto(Usuario auditor) {
        return Stream.of(auditor.getNombre(), auditor.getApellidos())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
    }

    @Named("principales")
    default List<String> principales(Set<EspecialidadAuditor> especialidades) {
        if (especialidades == null) {
            return List.of();
        }
        return especialidades.stream()
                .sorted()
                .limit(MAX_ESPECIALIDADES_PRINCIPALES)
                .map(Enum::name)
                .toList();
    }
}
