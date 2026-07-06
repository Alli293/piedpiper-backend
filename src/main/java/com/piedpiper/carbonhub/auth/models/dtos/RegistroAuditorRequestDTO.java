package com.piedpiper.carbonhub.auth.models.dtos;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAuditorRequestDTO {

    private String idToken;
    private String nombreCompleto;
    private String numeroCertificacion;
    private String entidadCertificadora;
    private LocalDate fechaVigenciaCert;
    private Integer aniosExperiencia;
    private boolean aceptaTerminos;
}
