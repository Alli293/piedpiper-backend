package com.piedpiper.carbonhub.ima.mappers;

import com.piedpiper.carbonhub.ima.models.dtos.ImaResponseDTO;
import com.piedpiper.carbonhub.ima.models.entities.ImaSnapshot;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImaSnapshotMapper {

    ImaResponseDTO toDto(ImaSnapshot snapshot);
}
