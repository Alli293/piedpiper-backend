package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaVerificadaDTO {

    private BigDecimal calificacion;
    private String comentario;
    private LocalDate fechaCalificacion;
}
