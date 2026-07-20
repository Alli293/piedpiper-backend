package com.piedpiper.carbonhub.invitacion.mappers;

import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionResponseDTO;
import com.piedpiper.carbonhub.invitacion.models.entities.Invitacion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvitacionMapper {

    @Mapping(target = "estado", ignore = true)
    InvitacionResponseDTO toDto(Invitacion invitacion);
}
