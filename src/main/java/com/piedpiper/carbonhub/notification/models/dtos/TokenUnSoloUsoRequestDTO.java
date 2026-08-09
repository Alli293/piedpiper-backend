package com.piedpiper.carbonhub.notification.models.dtos;

import com.piedpiper.carbonhub.notification.TokenVerificacionGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUnSoloUsoRequestDTO {

    @NotBlank(message = "El token es obligatorio.")
    @Pattern(regexp = TokenVerificacionGenerator.PATRON_TOKEN,
            message = "El token no tiene un formato válido.")
    private String token;
}
