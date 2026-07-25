package com.piedpiper.carbonhub.invitacion.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionPublicaResponseDTO {

    private String email;
    private String nombreEmpresa;
}
