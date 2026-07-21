package com.piedpiper.carbonhub.auditor.mappers;

import com.piedpiper.carbonhub.auditor.models.dtos.AuditorResumenResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface AuditorDirectorioMapper {

    int MAX_ESPECIALIDADES_PRINCIPALES = 3;

    @Mapping(target = "auditorId", source = "auditor.id")
    @Mapping(target = "nombre",
            expression = "java(perfil.getAuditor().getNombre() + \" \" + perfil.getAuditor().getApellidos())")
    @Mapping(target = "especialidadesPrincipales", source = "especialidades", qualifiedByName = "principales")
    AuditorResumenResponseDTO aResumen(PerfilAuditor perfil);

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
