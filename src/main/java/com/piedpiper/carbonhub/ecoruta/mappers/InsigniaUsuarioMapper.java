package com.piedpiper.carbonhub.ecoruta.mappers;

import com.piedpiper.carbonhub.ecoruta.models.dtos.InsigniaUsuarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.InsigniaUsuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsigniaUsuarioMapper {

    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "descripcion", ignore = true)
    InsigniaUsuarioResponseDTO toDto(InsigniaUsuario insigniaUsuario);
}
