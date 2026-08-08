package com.piedpiper.carbonhub.ecoruta.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparacionResponseDTO {

    private String actividadOriginalNombre;
    private Integer ecoScoreOriginal;
    private String categoriaTuristica;
    private String provincia;
    private List<AlternativaDTO> alternativas;
    private String mensaje;
}
