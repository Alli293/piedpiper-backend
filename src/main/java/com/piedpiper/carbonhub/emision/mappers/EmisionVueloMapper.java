package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmisionVueloMapper {

    @Mapping(target = "electricityValue", ignore = true)
    @Mapping(target = "electricityUnit", ignore = true)
    EmisionResponseDTO toDto(EmisionVuelo emision);
}
