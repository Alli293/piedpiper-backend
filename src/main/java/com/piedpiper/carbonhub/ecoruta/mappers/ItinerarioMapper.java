package com.piedpiper.carbonhub.ecoruta.mappers;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioActividadResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioDiaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResumenResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItinerarioMapper {

    @Mapping(target = "tipoViaje", expression = "java(entidad.getTipoViaje().name())")
    @Mapping(target = "estado", expression = "java(entidad.getEstado().name())")
    @Mapping(target = "clasificacionAmbiental",
            expression = "java(entidad.getClasificacionAmbiental() != null "
                    + "? entidad.getClasificacionAmbiental().name() : null)")
    @Mapping(target = "establecimientosEvaluados", ignore = true)
    ItinerarioResponseDTO toDto(Itinerario entidad);

    @Mapping(target = "tipoViaje", expression = "java(entidad.getTipoViaje().name())")
    @Mapping(target = "clasificacionAmbiental",
            expression = "java(entidad.getClasificacionAmbiental() != null "
                    + "? entidad.getClasificacionAmbiental().name() : null)")
    @Mapping(target = "provinciasVisitadas", ignore = true)
    ItinerarioResumenResponseDTO toResumenDto(Itinerario entidad);

    ItinerarioDiaResponseDTO toDto(ItinerarioDia entidad);

    @Mapping(target = "moneda",
            expression = "java(entidad.getMoneda() != null ? entidad.getMoneda().name() : null)")
    @Mapping(target = "empresaId",
            expression = "java(entidad.getEmpresa() != null ? entidad.getEmpresa().getId() : null)")
    @Mapping(target = "provincia", expression = "java(entidad.getProvincia().name())")
    @Mapping(target = "puntuacionAmbiental", ignore = true)
    ItinerarioActividadResponseDTO toDto(ItinerarioActividad entidad);
}
