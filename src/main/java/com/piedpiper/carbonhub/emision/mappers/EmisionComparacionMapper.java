package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmisionComparacionMapper {

    @Mapping(target = "anio", source = "anio")
    @Mapping(target = "huellaAcumuladaT", source = "huellaAcumuladaT")
    @Mapping(target = "limiteT", source = "limiteT")
    @Mapping(target = "porcentajeConsumido", source = "porcentajeConsumido")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "mensaje", source = "mensaje")
    ComparacionEmisionesResponseDTO toDto(Integer anio,
                                          BigDecimal huellaAcumuladaT,
                                          BigDecimal limiteT,
                                          BigDecimal porcentajeConsumido,
                                          String estado,
                                          String mensaje);
}
