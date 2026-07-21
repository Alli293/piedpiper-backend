package com.piedpiper.carbonhub.user.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaPerfilResponseDTO {

    private String nombreEmpresa;
    private String sectorIndustrial;
    private String pais;
    private Integer cantidadEmpleados;
}
