package com.piedpiper.carbonhub.auth.models.dtos;

import com.piedpiper.carbonhub.user.models.enums.MetodoAuth;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotNull(message = "El método de inicio de sesión es obligatorio.")
    private MetodoAuth metodo;
    private String idToken;
    private String email;
    private String contrasena;
}
