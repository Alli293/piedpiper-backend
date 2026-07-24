package com.piedpiper.carbonhub.auth.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidarTokenResetResponseDTO {

    private String email;
}
