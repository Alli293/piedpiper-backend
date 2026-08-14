package com.piedpiper.carbonhub.ecoruta.models.dtos;

import com.piedpiper.carbonhub.ecoruta.models.enums.ClasificacionAmbiental;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado del cálculo de EcoScore de un itinerario (PP-91). Nunca se expone directamente al
 * cliente — {@link com.piedpiper.carbonhub.ecoruta.service.EcoScoreService} lo produce para que
 * el servicio de itinerarios vuelque sus valores sobre la entidad {@code Itinerario}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcoScoreResultado {

    private BigDecimal ecoScore;
    private ClasificacionAmbiental clasificacion;
    private boolean parcial;
}
