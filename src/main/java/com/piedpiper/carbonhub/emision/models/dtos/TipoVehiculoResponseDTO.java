package com.piedpiper.carbonhub.emision.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoVehiculoResponseDTO {

    private String id;
    private String nombre;
    private List<CombustibleResponseDTO> combustibles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombustibleResponseDTO {
        private String id;
        private String nombre;
    }
}
