package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.Combustible;
import com.piedpiper.carbonhub.emision.models.enums.TipoVehiculo;
import com.piedpiper.carbonhub.emision.models.enums.UnidadDistancia;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarFlotaRequestDTO {

    @NotBlank(message = "Ingrese un título para este registro.")
    @Size(max = 150, message = "El título no puede contener más de 150 caracteres")
    private String titulo;

    @NotNull(message = "Seleccione un tipo de vehículo.")
    private TipoVehiculo tipoVehiculo;

    @NotNull(message = "Seleccione un combustible válido para este tipo de vehículo.")
    private Combustible combustible;

    @NotNull(message = "Ingrese una distancia mayor que 0.")
    @DecimalMin(value = "0", inclusive = false, message = "Ingrese una distancia mayor que 0.")
    @Digits(integer = 9, fraction = 3, message = "Ingrese una distancia mayor que 0.")
    private BigDecimal distanceValue;

    @NotNull(message = "Seleccione una unidad válida.")
    private UnidadDistancia distanceUnit;

    @NotNull(message = "Ingrese una fecha para este registro. ")
    @PastOrPresent(message = "La fecha no puede ser posterior a hoy.")
    private LocalDate fechaActividad;
}
