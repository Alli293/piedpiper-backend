package com.piedpiper.carbonhub.limite.mappers;

import com.piedpiper.carbonhub.limite.models.dtos.LimiteEmisionesResponseDTO;
import com.piedpiper.carbonhub.limite.models.entities.LimiteEmisiones;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LimiteEmisionesMapper {

    @Mapping(target = "mensaje", ignore = true)
    @Mapping(target = "recienCreada", ignore = true)
    LimiteEmisionesResponseDTO toDto(LimiteEmisiones limite);
}
