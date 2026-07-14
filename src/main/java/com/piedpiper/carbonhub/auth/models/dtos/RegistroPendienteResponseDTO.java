package com.piedpiper.carbonhub.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPendienteResponseDTO {

    private String mensaje;
    private String email;
}
