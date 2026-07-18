package com.piedpiper.carbonhub.emision.mappers;

import com.piedpiper.carbonhub.emision.models.dtos.ComparacionEmisionesResponseDTO;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EmisionComparacionMapper {

    public ComparacionEmisionesResponseDTO toDto(Integer anio,
                                                 BigDecimal huellaAcumuladaT,
                                                 BigDecimal limiteT,
                                                 BigDecimal porcentajeConsumido,
                                                 String estado,
                                                 String mensaje) {
        return new ComparacionEmisionesResponseDTO(
                anio,
                huellaAcumuladaT,
                limiteT,
                porcentajeConsumido,
                estado,
                mensaje);
    }
}
