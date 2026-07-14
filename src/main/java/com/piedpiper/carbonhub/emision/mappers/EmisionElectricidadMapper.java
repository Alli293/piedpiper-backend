package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionElectricidad;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisionElectricidadMapper {

    EmisionResponseDTO toDto(EmisionElectricidad emision);
}
