package com.piedpiper.carbonhub.auditor.mappers;

import com.piedpiper.carbonhub.auditor.models.dtos.PerfilAuditorResponseDTO;
import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PerfilAuditorMapper {

    @Mapping(target = "auditorId", source = "auditor.id")
    @Mapping(target = "especialidades", source = "especialidades", qualifiedByName = "csvToList")
    @Mapping(target = "zonasCobertura", source = "zonasCobertura", qualifiedByName = "csvToList")
    PerfilAuditorResponseDTO aResponseDto(PerfilAuditor perfil);

    @Named("csvToList")
    default List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    default String listToCsv(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }
}
