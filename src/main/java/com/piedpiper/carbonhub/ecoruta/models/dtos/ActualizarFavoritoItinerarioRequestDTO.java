package com.piedpiper.carbonhub.ecoruta.models.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarFavoritoItinerarioRequestDTO {

    @NotNull(message = "Indica si el itinerario debe quedar marcado como favorito.")
    private Boolean favorito;
}
