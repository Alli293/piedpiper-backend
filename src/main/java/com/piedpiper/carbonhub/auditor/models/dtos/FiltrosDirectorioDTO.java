package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltrosDirectorioDTO {

    private String terminoBusqueda;
    private List<String> especialidades;
    private String zonaGeografica;
    private BigDecimal calificacionMinima;
    private Boolean soloDisponibles;
    private int pagina;
    private Integer tamanioPagina;
    private String ordenamiento;
}
