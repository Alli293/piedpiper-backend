package com.piedpiper.carbonhub.empresa.mappers;

import com.piedpiper.carbonhub.empresa.models.dtos.ConfiguracionInicialEmpresaResponseDTO;
import com.piedpiper.carbonhub.empresa.models.dtos.EmpresaReporteDTO;
import com.piedpiper.carbonhub.empresa.models.entities.Empresa;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "empresaId", source = "id")
    @Mapping(target = "documentosPendientes", ignore = true)
    @Mapping(target = "recienCreada", ignore = true)
    ConfiguracionInicialEmpresaResponseDTO toDto(Empresa empresa);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombreEmpresa", source = "nombreEmpresa")
    EmpresaReporteDTO toReporteDto(Empresa empresa);
}
