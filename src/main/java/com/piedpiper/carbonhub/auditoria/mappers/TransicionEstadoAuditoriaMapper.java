package com.piedpiper.carbonhub.auditoria.mappers;

import com.piedpiper.carbonhub.auditoria.models.dtos.TransicionEstadoAuditoriaResponseDTO;
import com.piedpiper.carbonhub.auditoria.models.entities.TransicionEstadoAuditoria;
import com.piedpiper.carbonhub.auditoria.models.enums.ActorTransicionAuditoria;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransicionEstadoAuditoriaMapper {

    String RESPONSABLE_AUTOMATICO = "Proceso automático";

    @Mapping(target = "responsable", source = "transicion", qualifiedByName = "responsableLegible")
    TransicionEstadoAuditoriaResponseDTO toDto(TransicionEstadoAuditoria transicion);

    List<TransicionEstadoAuditoriaResponseDTO> toDtos(List<TransicionEstadoAuditoria> transiciones);

    /**
     * Las transiciones del proceso automatico no tienen persona detras, y el historial no puede
     * mostrar un hueco en la columna de responsable: la historia pide que cada entrada diga quien la
     * origino. El respaldo cubre tambien el caso de una transicion de persona cuyo nombre no se pudo
     * resolver, que sin esto quedaria como una linea sin responsable.
     */
    @Named("responsableLegible")
    default String responsableLegible(TransicionEstadoAuditoria transicion) {
        if (transicion.getActor() == ActorTransicionAuditoria.SISTEMA) {
            return RESPONSABLE_AUTOMATICO;
        }
        String nombre = transicion.getResponsableNombre();
        return nombre == null || nombre.isBlank() ? RESPONSABLE_AUTOMATICO : nombre;
    }
}
