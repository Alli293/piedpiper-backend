package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasViajeResponseDTO {

    private UUID id;
    private Integer cantidadDias;
    private LocalDate fechaInicio;
    private String tipoViaje;
    private String presupuesto;
    private List<String> intereses;
    private String provinciaPreferida;
    private String ubicacionActual;
    private boolean buscarCercaDeMi;
    private String limitacionesMovilidad;
    private boolean requiereHospedaje;
    private boolean conversacionCompleta;
    private boolean recienCreada;
}
