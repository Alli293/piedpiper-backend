package com.piedpiper.carbonhub.auditoria.mappers;

import com.piedpiper.carbonhub.auditoria.models.dtos.AuditorAsignadoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface SolicitudAuditoriaMapper {

    @Mapping(target = "idAuditor", source = "auditor.id")
    SolicitudAuditoriaResponseDTO toDto(SolicitudAuditoria solicitud);

    DocumentoRespaldoResponseDTO toDto(DocumentoRespaldo documento);

    @Mapping(target = "nombre", source = "auditor", qualifiedByName = "nombreCompleto")
    AuditorAsignadoResponseDTO toDto(Usuario auditor);

    @Named("nombreCompleto")
    default String nombreCompleto(Usuario auditor) {
        String nombreCompleto = Stream.of(auditor.getNombre(), auditor.getApellidos())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        return nombreCompleto.isBlank() ? auditor.getNombreVisible() : nombreCompleto;
    }
}
