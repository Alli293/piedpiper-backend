package com.piedpiper.carbonhub.ecoruta.mappers;

import com.piedpiper.carbonhub.ecoruta.models.dtos.PreferenciasViajeResponseDTO;
import com.piedpiper.carbonhub.ecoruta.models.entities.PreferenciasViaje;
import com.piedpiper.carbonhub.ecoruta.models.enums.InteresTuristico;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PreferenciasViajeMapper {

    @Mapping(target = "tipoViaje", expression = "java(entidad.getTipoViaje().name())")
    @Mapping(target = "provinciaPreferida",
            expression = "java(entidad.getProvinciaPreferida() != null ? entidad.getProvinciaPreferida().name() : null)")
    @Mapping(target = "intereses", expression = "java(mapearIntereses(entidad.getIntereses()))")
    @Mapping(target = "conversacionCompleta", ignore = true)
    @Mapping(target = "recienCreada", ignore = true)
    PreferenciasViajeResponseDTO toDto(PreferenciasViaje entidad);

    default List<String> mapearIntereses(List<InteresTuristico> intereses) {
        return intereses.stream().map(Enum::name).toList();
    }
}
