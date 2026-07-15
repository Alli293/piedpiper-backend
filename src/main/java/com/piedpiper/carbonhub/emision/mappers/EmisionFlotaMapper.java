package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.EmisionFlotaResponseDTO;
import com.piedpiper.carbonhub.emision.models.entities.EmisionFlota;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmisionFlotaMapper {

    EmisionFlotaResponseDTO toDto(EmisionFlota emision);
}
