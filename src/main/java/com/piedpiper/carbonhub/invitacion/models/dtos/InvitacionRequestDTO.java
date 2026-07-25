package com.piedpiper.carbonhub.invitacion.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionRequestDTO {

    @NotBlank(message = "Ingresa un correo electrónico válido")
    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 254, message = "Ingresa un correo electrónico válido")
    private String email;
}
