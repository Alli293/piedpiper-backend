package com.piedpiper.carbonhub.auditoria.mappers;

import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SolicitudAuditoriaMapper {

    SolicitudAuditoriaResponseDTO toDto(SolicitudAuditoria solicitud);

    DocumentoRespaldoResponseDTO toDto(DocumentoRespaldo documento);
}
