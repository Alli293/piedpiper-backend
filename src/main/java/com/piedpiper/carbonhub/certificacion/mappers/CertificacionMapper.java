package com.piedpiper.carbonhub.certificacion.mappers;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificacionMapper {

    @Mapping(source = "empresa.id", target = "idEmpresa")
    @Mapping(source = "auditor.id", target = "idAuditor")
    @Mapping(target = "nombreCertificacion", ignore = true)
    @Mapping(target = "recienEmitida", ignore = true)
    CertificacionResponseDTO toDto(Certificacion certificacion);
}
