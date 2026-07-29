package com.piedpiper.carbonhub.insignia.mappers;

import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsigniaEmpresaMapper {

    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "descripcion", ignore = true)
    InsigniaEmpresaResponseDTO toDto(InsigniaEmpresa insigniaEmpresa);
}
