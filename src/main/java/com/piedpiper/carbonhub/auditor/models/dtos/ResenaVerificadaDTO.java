package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaVerificadaDTO {

    private UUID id;
    private UUID empresaId;
    private BigDecimal calificacion;
    private String comentario;
    private LocalDate fechaCalificacion;
    private String nombreCalificador;
    private String nombreEmpresa;
}
