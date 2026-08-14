package com.piedpiper.carbonhub.auditor.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificacionPublicaDTO {

    private String nombre;
    private String entidadCertificadora;
    private LocalDate fechaVigencia;
    private boolean vencida;
}
