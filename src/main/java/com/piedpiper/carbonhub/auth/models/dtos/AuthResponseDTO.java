package com.piedpiper.carbonhub.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String rol;
    private String estado;
    private String redirect;
}
