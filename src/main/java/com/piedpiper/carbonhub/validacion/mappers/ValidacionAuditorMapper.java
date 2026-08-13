package com.piedpiper.carbonhub.validacion.mappers;

import com.piedpiper.carbonhub.auditor.models.entities.PerfilAuditor;
import com.piedpiper.carbonhub.auditor.models.enums.EspecialidadAuditor;
import com.piedpiper.carbonhub.user.models.entities.Usuario;
import com.piedpiper.carbonhub.validacion.models.dtos.DocumentoCredencialResumenResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.MiSolicitudAuditorResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudDetalleResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudPendienteResponseDTO;
import com.piedpiper.carbonhub.validacion.models.dtos.SolicitudResueltaResponseDTO;
import com.piedpiper.carbonhub.validacion.models.entities.SolicitudValidacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ValidacionAuditorMapper {

    @Mapping(target = "nombreAuditor", source = "auditor", qualifiedByName = "nombreCompleto")
    @Mapping(target = "email", source = "auditor.email")
    SolicitudPendienteResponseDTO aPendienteDto(SolicitudValidacion solicitud);

    @Mapping(target = "estadoAuditor", source = "auditor.estado")
    SolicitudResueltaResponseDTO aResueltaDto(SolicitudValidacion solicitud);

    MiSolicitudAuditorResponseDTO aMiSolicitudDto(SolicitudValidacion solicitud);

    @Mapping(target = "id", source = "solicitud.id")
    @Mapping(target = "nombreAuditor", source = "solicitud.auditor", qualifiedByName = "nombreCompleto")
    @Mapping(target = "email", source = "solicitud.auditor.email")
    @Mapping(target = "estado", source = "solicitud.estado")
    @Mapping(target = "fechaSolicitud", source = "solicitud.fechaSolicitud")
    @Mapping(target = "aniosExperiencia", source = "perfil.aniosExperiencia")
    @Mapping(target = "especialidades", source = "perfil", qualifiedByName = "especialidadesOrdenadas")
    @Mapping(target = "descripcionProfesional", source = "perfil.descripcionProfesional")
    @Mapping(target = "sitioWeb", source = "perfil.sitioWeb")
    @Mapping(target = "documentos", source = "documentos")
    SolicitudDetalleResponseDTO aDetalleDto(SolicitudValidacion solicitud, PerfilAuditor perfil,
                                            List<DocumentoCredencialResumenResponseDTO> documentos);

    @Named("nombreCompleto")
    default String nombreCompleto(Usuario auditor) {
        String nombre = auditor.getNombre() == null ? "" : auditor.getNombre();
        String apellidos = auditor.getApellidos() == null ? "" : " " + auditor.getApellidos();
        return (nombre + apellidos).trim();
    }

    @Named("especialidadesOrdenadas")
    default List<String> especialidadesOrdenadas(PerfilAuditor perfil) {
        if (perfil == null || perfil.getEspecialidades() == null) {
            return List.of();
        }
        return perfil.getEspecialidades().stream()
                .sorted(Comparator.comparing(EspecialidadAuditor::name))
                .map(Enum::name)
                .toList();
    }
}
