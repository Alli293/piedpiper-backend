package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilAuditorResponseDTO {

    private UUID auditorId;
    private List<String> especialidades;
    private List<String> zonasCobertura;
    private boolean disponible;
    private String descripcionProfesional;
    private Instant actualizadoEn;
}
