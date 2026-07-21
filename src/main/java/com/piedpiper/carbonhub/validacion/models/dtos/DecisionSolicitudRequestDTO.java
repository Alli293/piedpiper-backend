package com.piedpiper.carbonhub.validacion.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionSolicitudRequestDTO {

    @NotBlank(message = "La decisión es obligatoria.")
    private String decision;

    private String motivoRechazo;
}
