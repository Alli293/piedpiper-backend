package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditorResumenResponseDTO {

    private UUID auditorId;
    private String nombre;
    private String fotoPerfil;
    private List<String> especialidadesPrincipales;
    private BigDecimal calificacionPromedio;
    private int totalResenas;
    private boolean disponible;
    private Integer auditoriasCompletadas;
    private Integer aniosExperiencia;
    private String provincia;
}
