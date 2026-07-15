package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.VueloResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionVuelo;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisionVueloMapper {

    VueloResponseDTO toDto(EmisionVuelo emision);
}
