package com.piedpiper.carbonhub.auth.mappers;

import com.piedpiper.carbonhub.auth.models.dtos.AuthResponseDTO;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioAuthMapper {

    @Mapping(target = "rol", expression = "java(usuario.getRol().name())")
    @Mapping(target = "estado", expression = "java(usuario.getEstado().name())")
    AuthResponseDTO toAuthResponse(Usuario usuario, String token, String redirect);
}
