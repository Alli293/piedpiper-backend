package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.CabinClass;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarVueloRequestDTO {

    @NotNull(message = "Ingrese al menos 1 pasajero.")
    @Min(value = 1, message = "Ingrese al menos 1 pasajero.")
    @Max(value = 1000, message = "Ingrese como máximo 1000 pasajeros.")
    private Integer passengers;

    @NotEmpty(message = "Agregue al menos un trayecto.")
    @Size(max = 10, message = "Ingrese como máximo 10 trayectos.")
    @Valid
    private List<LegDTO> legs;

    private UnidadDistancia distanceUnit = UnidadDistancia.KM;

    @NotNull(message = "Ingrese la fecha del vuelo.")
    @PastOrPresent(message = "La fecha no puede ser posterior a hoy.")
    private LocalDate fechaActividad;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegDTO {

        @NotBlank(message = "Ingrese un código IATA de 3 letras.")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Ingrese un código IATA de 3 letras.")
        private String departureAirport;

        @NotBlank(message = "Ingrese un código IATA de 3 letras.")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Ingrese un código IATA de 3 letras.")
        private String destinationAirport;

        private CabinClass cabinClass = CabinClass.ECONOMY;

        @AssertTrue(message = "El origen y el destino no pueden ser iguales.")
        public boolean isAirportsDifferent() {
            if (departureAirport == null || destinationAirport == null) {
                return true;
            }
            return !departureAirport.equalsIgnoreCase(destinationAirport);
        }
    }
}
