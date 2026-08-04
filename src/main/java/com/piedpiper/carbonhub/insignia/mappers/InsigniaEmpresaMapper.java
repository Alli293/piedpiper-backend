package com.piedpiper.carbonhub.insignia.mappers;

import com.piedpiper.carbonhub.insignia.models.dtos.InsigniaEmpresaResponseDTO;
import com.piedpiper.carbonhub.insignia.models.entities.InsigniaEmpresa;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsigniaEmpresaMapper {

    @Mapping(source = "id", target = "idInsigniaEmpresa")
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "descripcion", ignore = true)
    @Mapping(target = "criteriosObtencion", ignore = true)
    @Mapping(target = "emisor", ignore = true)
    @Mapping(target = "receptor", ignore = true)
    @Mapping(target = "urlVerificacionPublica", ignore = true)
    @Mapping(target = "urlVerificacionJwt", ignore = true)
    @Mapping(target = "urlLinkedIn", ignore = true)
    InsigniaEmpresaResponseDTO toDto(InsigniaEmpresa insigniaEmpresa);
}
