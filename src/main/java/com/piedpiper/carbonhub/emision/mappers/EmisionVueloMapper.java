package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionVueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisionVueloMapper {

    EmisionVueloResponseDTO toDto(EmisionVuelo emision);
}
