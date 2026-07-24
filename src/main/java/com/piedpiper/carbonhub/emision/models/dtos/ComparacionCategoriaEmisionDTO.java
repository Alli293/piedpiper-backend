package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CategoriaEmision;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparacionCategoriaEmisionDTO {
    private CategoriaEmision categoria;
    private BigDecimal huellaT;
    private BigDecimal porcentaje;
}
