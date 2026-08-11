package com.piedpiper.carbonhub.calificacion.mappers;

import com.piedpiper.carbonhub.calificacion.models.dtos.CalificacionResponseDTO;
import com.piedpiper.carbonhub.calificacion.models.entities.Calificacion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CalificacionMapper {

    @Mapping(source = "auditoria.id", target = "auditoriaId")
    @Mapping(source = "auditor.id", target = "auditorId")
    @Mapping(source = "empresa.id", target = "empresaId")
    CalificacionResponseDTO toDto(Calificacion calificacion);
}
