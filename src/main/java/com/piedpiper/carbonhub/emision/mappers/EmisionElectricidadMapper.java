package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmisionElectricidadMapper {

    @Mapping(target = "passengers", ignore = true)
    @Mapping(target = "legs", ignore = true)
    @Mapping(target = "distanceUnit", ignore = true)
    @Mapping(target = "distanceValue", ignore = true)
    EmisionResponseDTO toDto(EmisionElectricidad emision);
}
