package com.piedpiper.carbonhub.ecoruta.mappers;

import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioActividadResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioDiaResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.dtos.ItinerarioResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.Itinerario;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioActividad;
import com.piedpiper.carbonhub.ecoruta.models.entities.ItinerarioDia;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItinerarioMapper {

    @Mapping(target = "tipoViaje", expression = "java(entidad.getTipoViaje().name())")
    @Mapping(target = "estado", expression = "java(entidad.getEstado().name())")
    ItinerarioResponseDTO toDto(Itinerario entidad);

    ItinerarioDiaResponseDTO toDto(ItinerarioDia entidad);

    @Mapping(target = "moneda",
            expression = "java(entidad.getMoneda() != null ? entidad.getMoneda().name() : null)")
    @Mapping(target = "provincia", expression = "java(entidad.getProvincia().name())")
    @Mapping(target = "puntuacionAmbiental", ignore = true)
    ItinerarioActividadResponseDTO toDto(ItinerarioActividad entidad);
}
