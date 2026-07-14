package com.piedpiper.carbonhub.emision.models.dtos;

import com.piedpiper.carbonhub.emision.models.enums.UnidadElectricidad;

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
public class RegistrarElectricidadRequestDTO {

    @NotBlank(message = "Ingrese un título para este registro.")
    @Size(max = 150, message = "El título no puede contener más de 150 caracteres")
    private String titulo;

    @NotNull(message = "Ingrese una cantidad.")
    @DecimalMin(value = "0", inclusive = false, message = "Ingrese una cantidad mayor que 0.")
    @Digits(integer = 12, fraction = 3, message = "Ingrese una cantidad mayor que 0.")
    private BigDecimal electricityValue;

    @NotNull(message = "Seleccione una unidad válida.")
    private UnidadElectricidad electricityUnit = UnidadElectricidad.KWH;

    @NotNull(message = "Ingrese una fecha para este registro. ")
    @PastOrPresent(message = "La fecha no puede ser posterior a hoy.")
    private LocalDate fechaActividad;
}
