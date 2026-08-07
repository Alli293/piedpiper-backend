package com.piedpiper.carbonhub.meta.mappers;

import com.piedpiper.carbonhub.meta.models.dtos.MetaResponseDTO;
import com.piedpiper.carbonhub.meta.models.entities.Meta;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MetaMapper {

    @Mapping(target = "huellaActualT", ignore = true)
    @Mapping(target = "progresoPorcentaje", ignore = true)
    @Mapping(target = "vencida", ignore = true)
    MetaResponseDTO toDto(Meta meta);
}
