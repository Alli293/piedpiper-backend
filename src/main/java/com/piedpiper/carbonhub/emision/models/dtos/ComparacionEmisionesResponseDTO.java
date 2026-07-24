package com.piedpiper.carbonhub.emision.models.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparacionEmisionesResponseDTO {
    private Integer anio;
    private BigDecimal huellaAcumuladaT;
    private BigDecimal limiteT;
    private BigDecimal porcentajeConsumido;
    private String estado;
    private String mensaje;
    private List<ComparacionCategoriaEmisionDTO> categorias;
}
