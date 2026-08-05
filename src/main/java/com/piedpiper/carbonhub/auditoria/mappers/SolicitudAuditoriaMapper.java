package com.piedpiper.carbonhub.auditoria.mappers;

import com.piedpiper.carbonhub.auditoria.models.dtos.AuditorAsignadoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.DocumentoRespaldoResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaDetalleResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.dtos.SolicitudAuditoriaResumenResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.DocumentoRespaldo;
import com.piedpiper.carbonhub.auditoria.models.entities.SolicitudAuditoria;
import com.piedpiper.carbonhub.user.models.entities.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;


@Mapper(componentModel = "spring")
public interface SolicitudAuditoriaMapper {

    @Mapping(target = "idAuditor", source = "auditor.id")
    SolicitudAuditoriaResponseDTO toDto(SolicitudAuditoria solicitud);

    @Mapping(target = "idAuditor", source = "auditor.id")
    @Mapping(target = "nombreEmpresa", source = "empresa.nombreEmpresa")
    @Mapping(target = "estadoDescripcion", source = "estado.descripcion")
    @Mapping(target = "historial", ignore = true)
    SolicitudAuditoriaDetalleResponseDTO toDetalleDto(SolicitudAuditoria solicitud);

    @Mapping(target = "idAuditor", source = "auditor.id")
    @Mapping(target = "nombreAuditor", source = "auditor", qualifiedByName = "nombreCompletoOpcional")
    @Mapping(target = "nombreEmpresa", source = "empresa.nombreEmpresa")
    @Mapping(target = "estadoDescripcion", source = "estado.descripcion")
    @Mapping(target = "cantidadDocumentos", source = "documentos", qualifiedByName = "cantidad")
    SolicitudAuditoriaResumenResponseDTO toResumenDto(SolicitudAuditoria solicitud);

    List<SolicitudAuditoriaResumenResponseDTO> toResumenDtos(List<SolicitudAuditoria> solicitudes);

    DocumentoRespaldoResponseDTO toDto(DocumentoRespaldo documento);

    @Mapping(target = "nombre", source = "auditor", qualifiedByName = "nombreCompleto")
    AuditorAsignadoResponseDTO toDto(Usuario auditor);

    @Named("nombreCompleto")
    default String nombreCompleto(Usuario auditor) {
        return auditor.nombreCompleto();
    }

    /** El listado incluye solicitudes sin auditor asignado, asi que el nombre puede no existir. */
    @Named("nombreCompletoOpcional")
    default String nombreCompletoOpcional(Usuario auditor) {
        return auditor == null ? null : auditor.nombreCompleto();
    }

    /**
     * Solo la cantidad: el listado no necesita los adjuntos, y mapearlos traeria su contenido
     * binario para cada fila.
     */
    @Named("cantidad")
    default int cantidad(List<DocumentoRespaldo> documentos) {
        return documentos == null ? 0 : documentos.size();
    }
}
