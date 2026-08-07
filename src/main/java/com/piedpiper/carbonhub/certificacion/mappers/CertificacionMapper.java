package com.piedpiper.carbonhub.certificacion.mappers;

import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionPublicaResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.dtos.CertificacionResumenResponseDTO;
import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificacionMapper {

    @Mapping(source = "empresa.id", target = "idEmpresa")
    @Mapping(source = "auditor.id", target = "idAuditor")
    @Mapping(target = "nombreCertificacion", ignore = true)
    @Mapping(target = "recienEmitida", ignore = true)
    @Mapping(target = "vigente", ignore = true)
    @Mapping(target = "urlVerificacion", ignore = true)
    CertificacionResponseDTO toDto(Certificacion certificacion);

    @Mapping(target = "nombreCertificacion", ignore = true)
    @Mapping(target = "nombreAuditor", ignore = true)
    @Mapping(target = "estado", ignore = true)
    CertificacionPublicaResponseDTO toPublicaDto(Certificacion certificacion);

    @Mapping(source = "empresa.id", target = "idEmpresa")
    @Mapping(source = "auditor.id", target = "idAuditor")
    @Mapping(target = "nombreCertificacion", ignore = true)
    @Mapping(target = "vigente", ignore = true)
    @Mapping(target = "urlVerificacion", ignore = true)
    CertificacionResumenResponseDTO toResumenDto(Certificacion certificacion);
}
