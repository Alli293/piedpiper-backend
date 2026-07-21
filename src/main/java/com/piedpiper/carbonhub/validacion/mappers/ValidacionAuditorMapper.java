package com.piedpiper.carbonhub.validacion.mappers;

import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudPendienteResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ValidacionAuditorMapper {

    @Mapping(target = "nombreAuditor", source = "auditor", qualifiedByName = "nombreCompleto")
    @Mapping(target = "email", source = "auditor.email")
    SolicitudPendienteResponseDTO aPendienteDto(SolicitudValidacion solicitud);

    @Mapping(target = "estadoAuditor", source = "auditor.estado")
    SolicitudResueltaResponseDTO aResueltaDto(SolicitudValidacion solicitud);

    @Named("nombreCompleto")
    default String nombreCompleto(Usuario auditor) {
        String nombre = auditor.getNombre() == null ? "" : auditor.getNombre();
        String apellidos = auditor.getApellidos() == null ? "" : " " + auditor.getApellidos();
        return (nombre + apellidos).trim();
    }
}
