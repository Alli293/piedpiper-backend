package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionEnvioResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionEnvio;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisionEnvioMapper {

    EmisionEnvioResponseDTO toDto(EmisionEnvio emision);
}
