package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarioDiaResponseDTO {

    private Integer numeroDia;
    private LocalDate fecha;
    private List<ItinerarioActividadResponseDTO> actividades;
}
