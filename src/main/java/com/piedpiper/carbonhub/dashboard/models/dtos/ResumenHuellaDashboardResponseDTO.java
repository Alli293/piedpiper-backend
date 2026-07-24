package com.piedpiper.carbonhub.dashboard.models.dtos;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenHuellaDashboardResponseDTO {
    private String periodoSeleccionado;
    private BigDecimal huellaTotalT;
    private BigDecimal variacionPorcentual;
    private boolean tieneDatos;
}
