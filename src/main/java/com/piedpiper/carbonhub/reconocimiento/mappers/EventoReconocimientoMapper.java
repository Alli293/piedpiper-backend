package com.piedpiper.carbonhub.reconocimiento.mappers;

import com.piedpiper.carbonhub.reconocimiento.models.dtos.EventoReconocimientoResponseDTO;
import com.piedpiper.carbonhub.reconocimiento.models.entities.EventoReconocimiento;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventoReconocimientoMapper {

    EventoReconocimientoResponseDTO toDto(EventoReconocimiento evento);
}
